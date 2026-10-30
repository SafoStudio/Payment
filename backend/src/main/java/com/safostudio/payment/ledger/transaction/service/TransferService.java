
package com.safostudio.payment.ledger.transaction.service;

import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.dto.CalculateFeeRequest;
import com.safostudio.payment.fee.service.FeeCalculator;
import com.safostudio.payment.fee.service.FeeService;
import com.safostudio.payment.ledger.audit.domain.AuditLog;
import com.safostudio.payment.ledger.audit.repository.AuditLogRepository;
import com.safostudio.payment.ledger.balance.domain.BalanceHistory;
import com.safostudio.payment.ledger.balance.repository.BalanceHistoryRepository;
import com.safostudio.payment.ledger.idempotency.service.IdempotencyService;
import com.safostudio.payment.ledger.transaction.domain.Transaction;
import com.safostudio.payment.ledger.transaction.dto.AdjustBalanceRequest;
import com.safostudio.payment.ledger.transaction.dto.TransactionResponse;
import com.safostudio.payment.ledger.transaction.dto.TransferRequest;
import com.safostudio.payment.ledger.transaction.repository.TransactionRepository;
import com.safostudio.payment.shared.config.SystemWalletsProperties;
import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.shared.security.AppUserDetails;
import com.safostudio.payment.shared.util.MoneyUtils;
import com.safostudio.payment.wallet.domain.Wallet;
import com.safostudio.payment.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
  private static final String OPERATION_ADJUSTMENT = "ADJUSTMENT";

  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final BalanceHistoryRepository balanceHistoryRepository;
  private final IdempotencyService idempotencyService;
  private final FeeCalculator feeCalculator;
  private final FeeService feeService;
  private final SystemWalletsProperties systemWalletsProperties;
  private final AuditLogRepository auditLogRepository;

  @Transactional
  public TransactionResponse transfer(TransferRequest request) {

    validateTransferRequest(request);

    String key = request.getIdempotencyKey();

    String fingerprint = idempotencyService.fingerprint(
            new TransferFingerprint(
                    request.getFromWalletId(),
                    request.getToWalletId(),
                    request.getAmount(),
                    request.getCurrency()
            )
    );

    IdempotencyService.Result idem =
            idempotencyService.tryStart(OPERATION_TRANSFER, key, fingerprint);

    switch (idem.outcome()) {
      case REPLAY -> {
        return replayTransaction(idem.response(), key);
      }
      case IN_PROGRESS ->
              throw BusinessException.idempotencyInProgress(key);
      case MISMATCH ->
              throw BusinessException.idempotencyKeyReuse(key);
      case PROCEED -> {
        // Continue
      }
    }

    Wallet fromWallet = walletRepository.findById(request.getFromWalletId())
            .orElseThrow(() ->
                    BusinessException.walletNotFound(request.getFromWalletId()));

    Wallet toWallet = walletRepository.findById(request.getToWalletId())
            .orElseThrow(() ->
                    BusinessException.walletNotFound(request.getToWalletId()));

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

      BigDecimal fromPrevious = fromWallet.getBalance();
      BigDecimal toPrevious = toWallet.getBalance();

      fromWallet.debit(request.getAmount());
      toWallet.credit(request.getAmount());

      walletRepository.save(fromWallet);
      walletRepository.save(toWallet);

      savedTransaction.complete();
      transactionRepository.save(savedTransaction);

      balanceHistoryRepository.save(BalanceHistory.create(
              fromWallet.getId(),
              fromPrevious,
              fromWallet.getBalance(),
              savedTransaction.getId(),
              "TRANSFER_DEBIT",
              fromWallet.getCurrency()
      ));

      balanceHistoryRepository.save(BalanceHistory.create(
              toWallet.getId(),
              toPrevious,
              toWallet.getBalance(),
              savedTransaction.getId(),
              "TRANSFER_CREDIT",
              toWallet.getCurrency()
      ));

      applyFees(savedTransaction, request);

      TransactionResponse response =
              TransactionResponse.fromDomain(savedTransaction);

      idempotencyService.complete(
              OPERATION_TRANSFER,
              key,
              savedTransaction.getId().toString()
      );

      return response;

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error(
              "Transfer failed for transaction {}",
              savedTransaction.getId(),
              e
      );

      throw BusinessException.transactionFailed(
              savedTransaction.getId()
      );
    }
  }

  @Transactional
  public TransactionResponse adjustBalance(AdjustBalanceRequest request) {

    validateAdjustmentRequest(request);

    BigDecimal amount = request.getAmount();

    boolean isCredit = amount.compareTo(BigDecimal.ZERO) > 0;
    BigDecimal absAmount = amount.abs();

    String key = request.getIdempotencyKey();

    String fingerprint = idempotencyService.fingerprint(
            new AdjustmentFingerprint(
                    request.getWalletId(),
                    amount,
                    request.getReason()
            )
    );

    IdempotencyService.Result idem =
            idempotencyService.tryStart(
                    OPERATION_ADJUSTMENT,
                    key,
                    fingerprint
            );

    switch (idem.outcome()) {
      case REPLAY -> {
        return replayTransaction(idem.response(), key);
      }
      case IN_PROGRESS ->
              throw BusinessException.idempotencyInProgress(key);
      case MISMATCH ->
              throw BusinessException.idempotencyKeyReuse(key);
      case PROCEED -> {
        // Continue
      }
    }

    Wallet wallet = walletRepository.findById(request.getWalletId())
            .orElseThrow(() ->
                    BusinessException.walletNotFound(request.getWalletId()));

    if (!wallet.isActive()) {
      throw BusinessException.walletNotActiveById(wallet.getId());
    }

    UUID systemWalletId =
            systemWalletsProperties.getCustomerWalletId();

    if (systemWalletId == null) {
      throw BusinessException.internalError(
              "Customer system wallet is not configured"
      );
    }

    if (isCredit) {
      // Credit: system wallet -> customer wallet
      // Debit: customer wallet -> system wallet
    } else {
      if (wallet.getBalance().compareTo(absAmount) < 0) {
        throw BusinessException.insufficientFunds(
                wallet.getId(),
                wallet.getBalance(),
                absAmount,
                wallet.getCurrency()
        );
      }
    }

    UUID debitWalletId = isCredit
            ? systemWalletId
            : wallet.getId();

    UUID creditWalletId = isCredit
            ? wallet.getId()
            : systemWalletId;

    Transaction transaction = Transaction.createAdjustment(
            debitWalletId,
            creditWalletId,
            absAmount,
            wallet.getCurrency(),
            request.getReason()
    );

    Transaction savedTransaction =
            transactionRepository.save(transaction);

    try {

      BigDecimal previous = wallet.getBalance();

      if (isCredit) {
        wallet.credit(absAmount);
      } else {
        wallet.debit(absAmount);
      }

      walletRepository.save(wallet);

      savedTransaction.complete();
      transactionRepository.save(savedTransaction);

      balanceHistoryRepository.save(BalanceHistory.create(
              wallet.getId(),
              previous,
              wallet.getBalance(),
              savedTransaction.getId(),
              isCredit
                      ? "ADJUSTMENT_CREDIT"
                      : "ADJUSTMENT_DEBIT",
              wallet.getCurrency()
      ));

      auditLogRepository.save(AuditLog.create(
              "WALLET",
              wallet.getId(),
              "ADJUST_BALANCE",
              currentUserId(),
              previous.toPlainString(),
              wallet.getBalance().toPlainString(),
              request.getIpAddress(),
              request.getUserAgent()
      ));

      TransactionResponse response =
              TransactionResponse.fromDomain(savedTransaction);

      idempotencyService.complete(
              OPERATION_ADJUSTMENT,
              key,
              savedTransaction.getId().toString()
      );

      return response;

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error(
              "Adjustment failed for transaction {}",
              savedTransaction.getId(),
              e
      );

      throw BusinessException.transactionFailed(
              savedTransaction.getId()
      );
    }
  }

  private TransactionResponse replayTransaction(
          String response,
          String key
  ) {

    UUID transactionId;

    try {
      transactionId = UUID.fromString(response);
    } catch (IllegalArgumentException e) {
      throw BusinessException.internalError(
              "Invalid idempotency response for key: " + key
      );
    }

    return transactionRepository.findById(transactionId)
            .map(TransactionResponse::fromDomain)
            .orElseThrow(() ->
                    BusinessException.internalError(
                            "Transaction not found for idempotency key: " + key
                    ));
  }

  private void applyFees(
          Transaction parentTransaction,
          TransferRequest request
  ) {

    CalculateFeeRequest feeRequest =
            CalculateFeeRequest.builder()
                    .transactionType(OPERATION_TRANSFER)
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

      feesWithTransaction.add(
              withParentTransaction(
                      fee,
                      parentTransaction.getId()
              )
      );

      applyFeeToWallets(
              fee,
              parentTransaction.getId()
      );

      createFeeTransaction(fee, parentTransaction);
    }

    feeService.saveAll(feesWithTransaction);
  }

  private Fee withParentTransaction(
          Fee fee,
          UUID parentTransactionId
  ) {

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

  private void applyFeeToWallets(
          Fee fee,
          UUID parentTransactionId
  ) {

    if (fee.getFromWalletId() != null) {

      Wallet from = walletRepository.findById(fee.getFromWalletId())
              .orElseThrow(() ->
                      BusinessException.walletNotFound(
                              fee.getFromWalletId()
                      ));

      BigDecimal previous = from.getBalance();

      from.debit(fee.getAmount());

      walletRepository.save(from);

      balanceHistoryRepository.save(BalanceHistory.create(
              from.getId(),
              previous,
              from.getBalance(),
              parentTransactionId,
              "FEE_DEBIT",
              from.getCurrency()
      ));
    }

    if (fee.getToWalletId() != null) {

      Wallet to = walletRepository.findById(fee.getToWalletId())
              .orElseThrow(() ->
                      BusinessException.walletNotFound(
                              fee.getToWalletId()
                      ));

      BigDecimal previous = to.getBalance();

      to.credit(fee.getAmount());

      walletRepository.save(to);

      balanceHistoryRepository.save(BalanceHistory.create(
              to.getId(),
              previous,
              to.getBalance(),
              parentTransactionId,
              "FEE_CREDIT",
              to.getCurrency()
      ));
    }
  }

  private void createFeeTransaction(
          Fee fee,
          Transaction parentTransaction
  ) {

    if (fee.getFromWalletId() == null
            || fee.getToWalletId() == null) {
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

    if (request.getFromWalletId() == null
            || request.getToWalletId() == null) {
      throw BusinessException.validationError(
              "Wallet IDs must not be null for transfer"
      );
    }

    if (request.getFromWalletId().equals(request.getToWalletId())) {
      throw BusinessException.sameWallet();
    }

    if (request.getIdempotencyKey() == null
            || request.getIdempotencyKey().isBlank()) {
      throw BusinessException.validationError(
              "Idempotency key is required"
      );
    }
  }

  private void validateWalletsForTransfer(
          Wallet fromWallet,
          Wallet toWallet,
          TransferRequest request
  ) {

    if (!fromWallet.getCurrency().equals(request.getCurrency())) {
      throw BusinessException.currencyMismatch(
              fromWallet.getCurrency(),
              request.getCurrency()
      );
    }

    if (!toWallet.getCurrency().equals(request.getCurrency())) {
      throw BusinessException.currencyMismatch(
              toWallet.getCurrency(),
              request.getCurrency()
      );
    }

    if (!fromWallet.isActive()) {
      throw BusinessException.walletNotActiveById(
              fromWallet.getId()
      );
    }

    if (!toWallet.isActive()) {
      throw BusinessException.walletNotActiveById(
              toWallet.getId()
      );
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

  private void validateAdjustmentRequest(AdjustBalanceRequest request) {

    if (request.getWalletId() == null) {
      throw BusinessException.validationError(
              "Wallet ID is required"
      );
    }

    if (request.getAmount() == null
            || request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
      throw BusinessException.validationError(
              "Amount must be non-zero"
      );
    }

    if (request.getReason() == null
            || request.getReason().isBlank()) {
      throw BusinessException.validationError(
              "Reason is required"
      );
    }

    if (request.getIdempotencyKey() == null
            || request.getIdempotencyKey().isBlank()) {
      throw BusinessException.validationError(
              "Idempotency key is required"
      );
    }
  }

  private UUID currentUserId() {

    Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
            || !(authentication.getPrincipal()
            instanceof AppUserDetails principal)) {

      throw BusinessException.validationError(
              "No authenticated principal"
      );
    }

    return principal.getUserId();
  }

  @Transactional(readOnly = true)
  public TransactionResponse getTransaction(UUID transactionId) {

    Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() ->
                    BusinessException.transactionNotFound(transactionId));

    return TransactionResponse.fromDomain(transaction);
  }

  @Transactional(readOnly = true)
  public List<TransactionResponse> getWalletTransactions(
          UUID walletId,
          int limit
  ) {

    if (!walletRepository.existsById(walletId)) {
      throw BusinessException.walletNotFound(walletId);
    }

    return transactionRepository.findRecentTransactions(walletId, limit)
            .stream()
            .map(TransactionResponse::fromDomain)
            .toList();
  }

  private record TransferFingerprint(
          UUID fromWalletId,
          UUID toWalletId,
          BigDecimal amount,
          String currency
  ) {}

  private record AdjustmentFingerprint(
          UUID walletId,
          BigDecimal amount,
          String reason
  ) {}
}
