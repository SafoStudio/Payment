package com.safostudio.payment.ledger.transaction.service;

import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.dto.CalculateFeeRequest;
import com.safostudio.payment.fee.service.FeeCalculator;
import com.safostudio.payment.fee.service.FeeService;
import com.safostudio.payment.ledger.idempotency.service.IdempotencyService;
import com.safostudio.payment.ledger.transaction.domain.Transaction;
import com.safostudio.payment.ledger.transaction.repository.TransactionRepository;
import com.safostudio.payment.shared.config.SystemWalletsProperties;
import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.shared.util.MoneyUtils;
import com.safostudio.payment.wallet.domain.Wallet;
import com.safostudio.payment.ledger.transaction.dto.TopUpRequest;
import com.safostudio.payment.ledger.transaction.dto.TransactionResponse;
import com.safostudio.payment.ledger.transaction.dto.TransferRequest;
import com.safostudio.payment.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

  private static final String OPERATION_TRANSFER = "TRANSFER";
  private static final String OPERATION_TOP_UP = "TOP_UP";

  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final IdempotencyService idempotencyService;
  private final FeeCalculator feeCalculator;
  private final FeeService feeService;
  private final SystemWalletsProperties systemWalletsProperties;

  @Transactional
  public TransactionResponse transfer(TransferRequest request) {
    validateTransferRequest(request);

    String key = request.getIdempotencyKey();
    String fingerprint = idempotencyService.fingerprint(new TransferFingerprint(
            request.getFromWalletId(),
            request.getToWalletId(),
            request.getAmount(),
            request.getCurrency()
    ));

    IdempotencyService.Result idem = idempotencyService.tryStart(OPERATION_TRANSFER, key, fingerprint);
    switch (idem.outcome()) {
      case REPLAY -> {
        return replayTransaction(idem.response(), key);
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

      applyFees(savedTransaction, request);

      TransactionResponse response = TransactionResponse.fromDomain(savedTransaction);
      idempotencyService.complete(OPERATION_TRANSFER, key, savedTransaction.getId().toString());
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

    String key = request.getIdempotencyKey();
    String fingerprint = idempotencyService.fingerprint(new TopUpFingerprint(
            request.getWalletId(),
            request.getAmount(),
            request.getCurrency()
    ));

    IdempotencyService.Result idem = idempotencyService.tryStart(OPERATION_TOP_UP, key, fingerprint);
    switch (idem.outcome()) {
      case REPLAY -> {
        return replayTransaction(idem.response(), key);
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
      idempotencyService.complete(OPERATION_TOP_UP, key, savedTransaction.getId().toString());
      return response;

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("Top-up failed for transaction {}", savedTransaction.getId(), e);
      throw BusinessException.transactionFailed(savedTransaction.getId());
    }
  }

  private TransactionResponse replayTransaction(String response, String key) {
    UUID transactionId;
    try {
      transactionId = UUID.fromString(response);
    } catch (IllegalArgumentException e) {
      throw BusinessException.internalError("Invalid idempotency response for key: " + key);
    }

    return transactionRepository.findById(transactionId)
            .map(TransactionResponse::fromDomain)
            .orElseThrow(() -> BusinessException.internalError(
                    "Transaction not found for idempotency key: " + key));
  }

  private void applyFees(Transaction parentTransaction, TransferRequest request) {
    CalculateFeeRequest feeRequest = CalculateFeeRequest.builder()
            .transactionType("OPERATION_TRANSFER")
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .fromWalletId(request.getFromWalletId())
            .toWalletId(request.getToWalletId())
            .build();

    List<Fee> fees = feeCalculator.calculateFees(feeRequest);
    if (fees.isEmpty()) {
      return;
    }

    List<Fee> feesWithTransaction = new ArrayList<>(fees.size());
    for (Fee fee : fees) {
      feesWithTransaction.add(withParentTransaction(fee, parentTransaction.getId()));
      applyFeeToWallets(fee);
      createFeeTransaction(fee, parentTransaction);
    }

    feeService.saveAll(feesWithTransaction);
  }

  private Fee withParentTransaction(Fee fee, UUID parentTransactionId) {
    return Fee.builder()
            .transactionId(parentTransactionId)
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
  }

  private void applyFeeToWallets(Fee fee) {
    if (fee.getFromWalletId() != null) {
      Wallet from = walletRepository.findById(fee.getFromWalletId())
              .orElseThrow(() -> BusinessException.walletNotFound(fee.getFromWalletId()));
      from.debit(fee.getAmount());
      walletRepository.save(from);
    }
    if (fee.getToWalletId() != null) {
      Wallet to = walletRepository.findById(fee.getToWalletId())
              .orElseThrow(() -> BusinessException.walletNotFound(fee.getToWalletId()));
      to.credit(fee.getAmount());
      walletRepository.save(to);
    }
  }

  private void createFeeTransaction(Fee fee, Transaction parentTransaction) {
    if (fee.getFromWalletId() == null || fee.getToWalletId() == null) {
      return;
    }
    Transaction feeTransaction = Transaction.createTransfer(
            fee.getFromWalletId(),
            fee.getToWalletId(),
            fee.getAmount(),
            fee.getCurrency(),
            "Fee for transaction: " + parentTransaction.getId()
    );
    feeTransaction.complete();
    transactionRepository.save(feeTransaction);
  }

  private void validateTransferRequest(TransferRequest request) {
    MoneyUtils.validateAmount(request.getAmount());
    MoneyUtils.validateCurrency(request.getCurrency());

    if (request.getFromWalletId() == null || request.getToWalletId() == null) {
      throw BusinessException.validationError("Wallet IDs must not be null for transfer");
    }
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
            .toList();
  }

  private record TransferFingerprint(UUID fromWalletId, UUID toWalletId, BigDecimal amount, String currency) {}
  private record TopUpFingerprint(UUID walletId, BigDecimal amount, String currency) {}
}