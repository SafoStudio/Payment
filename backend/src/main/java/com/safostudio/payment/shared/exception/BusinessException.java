package com.safostudio.payment.shared.exception;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode code;

  public BusinessException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public BusinessException(ErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public static BusinessException walletNotFound(UUID id) {
    return new BusinessException(ErrorCode.WALLET_NOT_FOUND, "Wallet not found: " + id);
  }

  public static BusinessException walletNotActive(Object status) {
    return new BusinessException(ErrorCode.WALLET_NOT_ACTIVE, "Wallet is not active. Current status: " + status);
  }

  public static BusinessException walletNotActiveById(UUID walletId) {
    return new BusinessException(ErrorCode.WALLET_NOT_ACTIVE, "Wallet is not active: " + walletId);
  }

  public static BusinessException walletAlreadyExists(UUID userId, String currency) {
    return new BusinessException(ErrorCode.WALLET_ALREADY_EXISTS,
            "Wallet with currency %s already exists for user: %s".formatted(currency, userId));
  }

  public static BusinessException walletClosed() {
    return new BusinessException(ErrorCode.WALLET_CLOSED, "Cannot credit closed wallet");
  }

  public static BusinessException walletNonZeroBalance(BigDecimal balance, String currency) {
    return new BusinessException(ErrorCode.WALLET_NON_ZERO_BALANCE,
            "Cannot close wallet with non-zero balance: %s %s".formatted(balance, currency));
  }

  public static BusinessException invalidAmount() {
    return new BusinessException(ErrorCode.INVALID_AMOUNT, "Amount must be positive");
  }

  public static BusinessException invalidInitialBalance(BigDecimal balance) {
    return new BusinessException(ErrorCode.INVALID_AMOUNT,
            "Initial balance cannot be negative: " + balance);
  }

  public static BusinessException invalidCurrency(String currency) {
    return new BusinessException(ErrorCode.INVALID_CURRENCY, "Invalid currency code: " + currency);
  }

  public static BusinessException currencyMismatch(String expected, String actual) {
    return new BusinessException(ErrorCode.CURRENCY_MISMATCH,
            "Currency mismatch. Expected: %s, Actual: %s".formatted(expected, actual));
  }

  public static BusinessException insufficientFunds(UUID walletId, BigDecimal available,
                                                    BigDecimal required, String currency) {
    return new BusinessException(ErrorCode.INSUFFICIENT_FUNDS,
            "Insufficient funds in wallet %s. Available: %s %s, Required: %s %s"
                    .formatted(walletId, available, currency, required, currency));
  }

  public static BusinessException insufficientFunds(BigDecimal available,
                                                    BigDecimal required, String currency) {
    return new BusinessException(ErrorCode.INSUFFICIENT_FUNDS,
            "Insufficient funds. Available: %s %s, Required: %s %s"
                    .formatted(available, currency, required, currency));
  }

  public static BusinessException sameWallet() {
    return new BusinessException(ErrorCode.SAME_WALLET, "Source and destination wallets must be different");
  }

  public static BusinessException duplicateIdempotencyKey(String key) {
    return new BusinessException(ErrorCode.DUPLICATE_IDEMPOTENCY_KEY, "Duplicate idempotency key: " + key);
  }

  public static BusinessException transactionNotFound(UUID id) {
    return new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND, "Transaction not found: " + id);
  }

  public static BusinessException transactionFailed(UUID id) {
    return new BusinessException(ErrorCode.TRANSACTION_FAILED, "Transaction failed: " + id);
  }

  public static BusinessException invalidTransactionStatus(Object expected, Object actual) {
    return new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS,
            "Invalid transaction status. Expected: %s, Actual: %s".formatted(expected, actual));
  }

  public static BusinessException invalidTransactionType(Object expected, Object actual) {
    return new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS,
            "Invalid transaction type. Expected: %s, Actual: %s".formatted(expected, actual));
  }

  public static BusinessException transactionNotPending(Object status) {
    return new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS,
            "Transaction is not pending. Current status: " + status);
  }

  public static BusinessException transactionAlreadyCompleted() {
    return new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS, "Transaction is already completed");
  }

  public static BusinessException invalidWallet(String operation, UUID walletId) {
    return new BusinessException(ErrorCode.VALIDATION_ERROR,
            "Invalid wallet for %s operation. Wallet: %s".formatted(operation, walletId));
  }

  public static BusinessException paymentNotFound(UUID id) {
    return new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found: " + id);
  }

  public static BusinessException paymentAlreadySucceeded() {
    return new BusinessException(ErrorCode.PAYMENT_ALREADY_SUCCEEDED, "Payment already succeeded");
  }

  public static BusinessException paymentCannotRefund(Object status) {
    return new BusinessException(ErrorCode.PAYMENT_CANNOT_REFUND,
            "Cannot refund payment with status: " + status);
  }

  public static BusinessException paymentCannotCancel(Object status) {
    return new BusinessException(ErrorCode.PAYMENT_CANNOT_CANCEL,
            "Cannot cancel payment with status: " + status);
  }

  public static BusinessException refundNotFound(UUID id) {
    return new BusinessException(ErrorCode.REFUND_NOT_FOUND, "Refund not found: " + id);
  }

  public static BusinessException refundExceedsPayment(BigDecimal refund, BigDecimal payment) {
    return new BusinessException(ErrorCode.REFUND_EXCEEDS_PAYMENT,
            "Refund amount %s exceeds payment amount %s".formatted(refund, payment));
  }

  public static BusinessException userNotFound(UUID id) {
    return new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found: " + id);
  }

  public static BusinessException userNotFoundByEmail(String email) {
    return new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found by email: " + email);
  }

  public static BusinessException userNotActive(UUID id, Object status) {
    return new BusinessException(ErrorCode.USER_NOT_ACTIVE,
            "User %s is not active. Current status: %s".formatted(id, status));
  }

  public static BusinessException userAlreadyExists(String field, String value) {
    return new BusinessException(ErrorCode.USER_ALREADY_EXISTS,
            "User with %s %s already exists".formatted(field, value));
  }

  public static BusinessException userCannotDeleteWithWallets(UUID id) {
    return new BusinessException(ErrorCode.USER_CANNOT_DELETE_WITH_WALLETS,
            "Cannot delete user %s with active wallets".formatted(id));
  }

  public static BusinessException feeRuleNotFound(UUID id) {
    return new BusinessException(ErrorCode.FEE_RULE_NOT_FOUND, "Fee rule not found: " + id);
  }

  public static BusinessException validationError(String message) {
    return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
  }

  public static BusinessException internalError(String message) {
    return new BusinessException(ErrorCode.INTERNAL_ERROR, message);
  }

  public static BusinessException invalidPaymentStatus(String action, Object status) {
    return new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS,
            "Cannot %s payment. Current status: %s".formatted(action, status));
  }

  public static BusinessException invalidRefundStatus(String action, Object status) {
    return new BusinessException(ErrorCode.INVALID_REFUND_STATUS,
            "Cannot %s refund. Current status: %s".formatted(action, status));
  }

  public static BusinessException refundAlreadyCompleted() {
    return new BusinessException(ErrorCode.INVALID_REFUND_STATUS, "Refund already completed");
  }

  public static BusinessException refundCannotReject(Object status) {
    return new BusinessException(ErrorCode.INVALID_REFUND_STATUS,
            "Cannot reject refund with status: " + status);
  }

  public static BusinessException userDeleted(UUID id) {
    return new BusinessException(ErrorCode.USER_NOT_ACTIVE,
            "User %s is deleted".formatted(id));
  }

  public static BusinessException idempotencyInProgress(String key) {
    return new BusinessException(ErrorCode.IDEMPOTENCY_IN_PROGRESS,
            "Request with idempotency key is in progress: " + key);
  }

  public static BusinessException idempotencyKeyReuse(String key) {
    return new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSE,
            "Idempotency key reused with different request payload: " + key);
  }

  public static BusinessException invalidRoleCodes(String message) {
    return new BusinessException(ErrorCode.INVALID_ROLE_CODES, message);
  }
}