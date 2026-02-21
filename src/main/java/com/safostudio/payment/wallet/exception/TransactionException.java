package com.safostudio.payment.wallet.exception;

import java.util.UUID;

public class TransactionException extends RuntimeException {

    private TransactionException(String message) {
        super(message);
    }

    public static TransactionException notFound(UUID id) {
        return new TransactionException("Transaction not found with id: %s".formatted(id));
    }

    public static TransactionException invalidStatus(Object expected, Object actual) {
        return new TransactionException("Invalid transaction status. Expected: %s, Actual: %s".formatted(expected, actual));
    }

    public static TransactionException alreadyCompleted() {
        return new TransactionException("Transaction is already completed");
    }

    public static TransactionException notPending(Object status) {
        return new TransactionException("Transaction is not pending. Current status: %s".formatted(status));
    }

    public static TransactionException invalidType(Object expected, Object actual) {
        return new TransactionException("Invalid transaction type. Expected: %s, Actual: %s".formatted(expected, actual));
    }

    public static TransactionException invalidWallet(String operation, UUID walletId) {
        return new TransactionException("Invalid wallet for %s operation. Wallet: %s".formatted(operation, walletId));
    }

    public static TransactionException amountMustBePositive() {
        return new TransactionException("Amount must be positive");
    }

    public static TransactionException currencyMismatch(String expected, String actual) {
        return new TransactionException("Currency mismatch. Expected: %s, Actual: %s".formatted(expected, actual));
    }
}