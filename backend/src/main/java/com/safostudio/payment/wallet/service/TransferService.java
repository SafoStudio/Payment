package com.safostudio.payment.wallet.service;

import tools.jackson.databind.json.JsonMapper;
import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.service.FeeCalculator;
import com.safostudio.payment.fee.service.FeeService;
import com.safostudio.payment.fee.dto.CalculateFeeRequest;
import com.safostudio.payment.ledger.idempotency.service.IdempotencyService;
import com.safostudio.payment.shared.config.SystemWalletsProperties;
import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.shared.util.MoneyUtils;
import com.safostudio.payment.ledger.transaction.domain.Transaction;
import com.safostudio.payment.wallet.domain.Wallet;
import com.safostudio.payment.ledger.transaction.repository.TransactionRepository;
import com.safostudio.payment.wallet.repository.WalletRepository;
import com.safostudio.payment.wallet.dto.TopUpRequest;
import com.safostudio.payment.wallet.dto.TransactionResponse;
import com.safostudio.payment.wallet.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final IdempotencyService idempotencyService;
  private final JsonMapper jsonMapper;
  private final FeeCalculator feeCalculator;
  private final FeeService feeService;
  private final SystemWalletsProperties systemWalletsProperties;

  @Transactional
  public TransactionResponse transfer(TransferRequest request) {
    validateTransferRequest(request);

    String operation = "TRANSFER";
    String key = request.getIdempotencyKey();
    String fingerprint = idempotencyService.fingerprint(new TransferFingerprint(
            request.getFromWalletId(),
            request.getToWalletId(),
            request.getAmount(),
            request.getCurrency()
    ));

    IdempotencyService.Result idem = idempotencyService.tryStart(operation, key, fingerprint);
    switch (idem.outcome()) {
      case REPLAY -> {
        return deserialize(idem.response(), TransactionResponse.class);
      }
      case IN_PROGRESS -> throw BusinessException.idempotencyInProgress(key);
      case MISMATCH -> throw BusinessException.idempotencyKeyReuse(key);
      case PROCEED -> { /* continue */ }
    }

    Wallet fromWallet = walletRepository.findById(request.getFromWalletId())
            .orElseThrow(() -> BusinessException.walletNotFound(request.getFromWalletId()));

    Wallet toWallet = walletRepository.findById(request.getToWalletId())
            .orElseThrow(() -> BusinessException.walletNotFound(request.getToWalletId()));

    validateWalletsForTransfer(fromWallet, toWallet, request);

    Transaction transaction = Transaction.createTransfer(
            fromWallet.getId(),
            toWallet.getId(),
            request.getAmount(),
            request.getCurrency(),
            request.getDescription()
    );

    Transaction savedTransaction = transactionRepository.save(transaction);

    try {
      fromWallet.debit(request.getAmount());
      toWallet.credit(request.getAmount());

      walletRepository.save(fromWallet);
      walletRepository.save(toWallet);

      savedTransaction.complete();
      transactionRepository.save(savedTransaction);

      CalculateFeeRequest feeRequest = CalculateFeeRequest.builder()
              .transactionType("TRANSFER")
              .amount(request.getAmount())
              .currency(request.getCurrency())
              .fromWalletId(request.getFromWalletId())
              .toWalletId(request.getToWalletId())
              .build();

      List<Fee> fees = feeCalculator.calculateFees(feeRequest);

      List<Fee> feesWithTransaction = new ArrayList<>(fees.size());
      for (Fee fee : fees) {
        Fee feeWithTransaction = Fee.builder()
                .transactionId(savedTransaction.getId())
                .fromWalletId(fee.getFromWalletId())
                .toWalletId(fee.getToWalletId())
                .amount(fee.getAmount())
                .currency(fee.getCurrency())
                .type(fee.getType())
                .percentage(fee.getPercentage())
                .fixedAmount(fee.getFixedAmount())
                .calculationRule(fee.getCalculationRule())
                .createdAt(Instant.now())
                .build();

        feesWithTransaction.add(feeWithTransaction);

        if (fee.getFromWalletId() != null) {
          Wallet feeWallet = walletRepository.findById(fee.getFromWalletId())
                  .orElseThrow(() -> BusinessException.walletNotFound(fee.getFromWalletId()));
          feeWallet.debit(fee.getAmount());
          walletRepository.save(feeWallet);
        }

        if (fee.getToWalletId() != null) {
          Wallet feeToWallet = walletRepository.findById(fee.getToWalletId())
                  .orElseThrow(() -> BusinessException.walletNotFound(fee.getToWalletId()));
          feeToWallet.credit(fee.getAmount());
          walletRepository.save(feeToWallet);
        }

        if (fee.getFromWalletId() != null && fee.getToWalletId() != null) {
          Transaction feeTransaction = Transaction.createTransfer(
                  fee.getFromWalletId(),
                  fee.getToWalletId(),
                  fee.getAmount(),
                  fee.getCurrency(),
                  "Fee for transaction: " + savedTransaction.getId()
          );
          feeTransaction.complete();
          transactionRepository.save(feeTransaction);
        }
      }

      if (!feesWithTransaction.isEmpty()) {
        feeService.saveAll(feesWithTransaction);
      }

      TransactionResponse response = TransactionResponse.fromDomain(savedTransaction);
      idempotencyService.complete(operation, key, response);
      return response;

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("Transfer failed for transaction {}", savedTransaction.getId(), e);
      throw BusinessException.transactionFailed(savedTransaction.getId());
    }
  }

  @Transactional
  public TransactionResponse topUp(TopUpRequest request) {
    MoneyUtils.validateAmount(request.getAmount());
    MoneyUtils.validateCurrency(request.getCurrency());

    String operation = "TOP_UP";
    String key = request.getIdempotencyKey();
    String fingerprint = idempotencyService.fingerprint(new TopUpFingerprint(
            request.getWalletId(),
            request.getAmount(),
            request.getCurrency()
    ));

    IdempotencyService.Result idem = idempotencyService.tryStart(operation, key, fingerprint);
    switch (idem.outcome()) {
      case REPLAY -> {
        return deserialize(idem.response(), TransactionResponse.class);
      }
      case IN_PROGRESS -> throw BusinessException.idempotencyInProgress(key);
      case MISMATCH -> throw BusinessException.idempotencyKeyReuse(key);
      case PROCEED -> { /* continue */ }
    }

    Wallet wallet = walletRepository.findById(request.getWalletId())
            .orElseThrow(() -> BusinessException.walletNotFound(request.getWalletId()));

    if (!wallet.getCurrency().equals(request.getCurrency())) {
      throw BusinessException.currencyMismatch(wallet.getCurrency(), request.getCurrency());
    }
    if (!wallet.isActive()) {
      throw BusinessException.walletNotActiveById(wallet.getId());
    }

    Transaction transaction = Transaction.createTopUp(
            systemWalletsProperties.getTopupWalletId(),
            wallet.getId(),
            request.getAmount(),
            request.getCurrency(),
            request.getDescription()
    );

    Transaction savedTransaction = transactionRepository.save(transaction);

    try {
      wallet.credit(request.getAmount());
      walletRepository.save(wallet);

      savedTransaction.complete();
      transactionRepository.save(savedTransaction);

      TransactionResponse response = TransactionResponse.fromDomain(savedTransaction);
      idempotencyService.complete(operation, key, response);
      return response;

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("Top-up failed for transaction {}", savedTransaction.getId(), e);
      throw BusinessException.transactionFailed(savedTransaction.getId());
    }
  }

  private void validateTransferRequest(TransferRequest request) {
    MoneyUtils.validateAmount(request.getAmount());
    MoneyUtils.validateCurrency(request.getCurrency());

    if (request.getFromWalletId().equals(request.getToWalletId())) {
      throw BusinessException.sameWallet();
    }
  }

  private void validateWalletsForTransfer(Wallet fromWallet, Wallet toWallet, TransferRequest request) {
    if (!fromWallet.getCurrency().equals(request.getCurrency())) {
      throw BusinessException.currencyMismatch(fromWallet.getCurrency(), request.getCurrency());
    }
    if (!toWallet.getCurrency().equals(request.getCurrency())) {
      throw BusinessException.currencyMismatch(toWallet.getCurrency(), request.getCurrency());
    }
    if (!fromWallet.isActive()) {
      throw BusinessException.walletNotActiveById(fromWallet.getId());
    }
    if (!toWallet.isActive()) {
      throw BusinessException.walletNotActiveById(toWallet.getId());
    }

    if (fromWallet.getBalance().compareTo(request.getAmount()) < 0) {
      throw BusinessException.insufficientFunds(
              fromWallet.getId(),
              fromWallet.getBalance(),
              request.getAmount(),
              request.getCurrency()
      );
    }
  }

  @Transactional(readOnly = true)
  public TransactionResponse getTransaction(UUID transactionId) {
    Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> BusinessException.transactionNotFound(transactionId));

    return TransactionResponse.fromDomain(transaction);
  }

  @Transactional(readOnly = true)
  public List<TransactionResponse> getWalletTransactions(UUID walletId, int limit) {
    if (!walletRepository.existsById(walletId)) {
      throw BusinessException.walletNotFound(walletId);
    }

    return transactionRepository.findRecentTransactions(walletId, limit).stream()
            .map(TransactionResponse::fromDomain)
            .collect(Collectors.toList());
  }

  private <T> T deserialize(String json, Class<T> type) {
    try {
      return jsonMapper.readValue(json, type);
    } catch (Exception e) {
      throw BusinessException.internalError("Failed to deserialize idempotent response");
    }
  }

  private record TransferFingerprint(UUID fromWalletId, UUID toWalletId, BigDecimal amount, String currency) {}
  private record TopUpFingerprint(UUID walletId, BigDecimal amount, String currency) {}
}