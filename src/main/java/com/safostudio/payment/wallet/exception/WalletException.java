package com.safostudio.payment.wallet.exception;

import java.math.BigDecimal;

public class WalletException extends RuntimeException {

    private WalletException(String message) {
        super(message);
    }

    public static WalletException insufficient(BigDecimal available, BigDecimal required, String currency) {
        return new WalletException(
                "Insufficient funds. Available: %s %s, Required: %s %s".formatted(
                        available, currency, required, currency)
        );
    }

    public static WalletException inactive(Object status) {
        return new WalletException("Wallet is not active. Current status: %s".formatted(status));
    }

    public static WalletException closed() {
        return new WalletException("Cannot credit closed wallet");
    }

    public static WalletException nonZeroBalance(BigDecimal balance, String currency) {
        return new WalletException("Cannot close wallet with non-zero balance: %s %s".formatted(balance, currency));
    }
}