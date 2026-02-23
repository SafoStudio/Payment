package com.safostudio.payment.wallet.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class TransferServiceException extends RuntimeException {

    private TransferServiceException(String message) {
        super(message);
    }

    public static TransferServiceException sameWallet() {
        return new TransferServiceException("Source and destination wallets must be different");
    }

    public static TransferServiceException invalidAmount() {
        return new TransferServiceException("Amount must be positive");
    }

    public static TransferServiceException invalidCurrency(String currency) {
        return new TransferServiceException("Invalid currency code: %s".formatted(currency));
    }

    public static TransferServiceException currencyMismatch(String expected, String actual) {
        return new TransferServiceException("Currency mismatch. Expected: %s, Actual: %s".formatted(expected, actual));
    }

    public static TransferServiceException walletNotActive(UUID walletId) {
        return new TransferServiceException("Wallet is not active: %s".formatted(walletId));
    }

    public static TransferServiceException insufficientFunds(UUID walletId, BigDecimal available,
                                                             BigDecimal required, String currency) {
        return new TransferServiceException(
                "Insufficient funds in wallet %s. Available: %s %s, Required: %s %s".formatted(
                        walletId, available, currency, required, currency)
        );
    }

    public static TransferServiceException walletNotFound(UUID walletId) {
        return new TransferServiceException("Wallet not found: %s".formatted(walletId));
    }

    public static TransferServiceException duplicateIdempotencyKey(String key) {
        return new TransferServiceException("Duplicate idempotency key: %s".formatted(key));
    }

    public static TransferServiceException transactionNotFound(UUID id) {
        return new TransferServiceException("Transaction not found with id: %s".formatted(id));
    }

    public static TransferServiceException transactionFailed(UUID id) {
        return new TransferServiceException("Transaction failed: %s".formatted(id));
    }
}