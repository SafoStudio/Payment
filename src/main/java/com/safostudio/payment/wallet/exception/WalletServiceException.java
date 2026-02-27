package com.safostudio.payment.wallet.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletServiceException extends RuntimeException {

    private WalletServiceException(String message) {
        super(message);
    }

    public static WalletServiceException notFound(UUID walletId) {
        return new WalletServiceException("Wallet not found with id: %s".formatted(walletId));
    }

    public static WalletServiceException alreadyExists(UUID userId, String currency) {
        return new WalletServiceException(
                "Wallet with currency %s already exists for owner: %s".formatted(currency, userId)
        );
    }

    public static WalletServiceException invalidCurrency(String currency) {
        return new WalletServiceException("Invalid currency code: %s".formatted(currency));
    }

    public static WalletServiceException invalidInitialBalance(BigDecimal balance) {
        return new WalletServiceException("Initial balance cannot be negative: %s".formatted(balance));
    }

    public static WalletServiceException notActive(UUID walletId) {
        return new WalletServiceException("Wallet is not active: %s".formatted(walletId));
    }

    public static WalletServiceException insufficientFunds(UUID walletId, BigDecimal available,
                                                           BigDecimal required, String currency) {
        return new WalletServiceException(
                "Insufficient funds in wallet %s. Available: %s %s, Required: %s %s".formatted(
                        walletId, available, currency, required, currency)
        );
    }

    public static WalletServiceException cannotCreditClosed(UUID walletId) {
        return new WalletServiceException("Cannot credit closed wallet: %s".formatted(walletId));
    }

    public static WalletServiceException cannotCloseWithBalance(UUID walletId, BigDecimal balance, String currency) {
        return new WalletServiceException(
                "Cannot close wallet %s with non-zero balance: %s %s".formatted(walletId, balance, currency)
        );
    }

    public static WalletServiceException userNotFound(UUID userId) {
        return new WalletServiceException("User not found: %s".formatted(userId));
    }

    public static WalletServiceException userNotActive(UUID userId, Object status) {
        return new WalletServiceException("User %s is not active. Current status: %s".formatted(userId, status));
    }
}