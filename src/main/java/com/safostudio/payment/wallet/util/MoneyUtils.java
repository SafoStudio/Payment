package com.safostudio.payment.wallet.util;

import com.safostudio.payment.wallet.exception.TransferServiceException;
import com.safostudio.payment.wallet.exception.WalletServiceException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public final class MoneyUtils {

    private MoneyUtils() {}

    public static final BigDecimal DEFAULT_AMOUNT = BigDecimal.ZERO;
    public static final int DEFAULT_SCALE = 4;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;

    /**
     * Validate amount (positive, not null)
     */
    public static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw TransferServiceException.invalidAmount();
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw TransferServiceException.invalidAmount();
        }
    }

    /**
     * Currency validation (ISO 4217 compliant)
     */
    public static void validateCurrency(String currencyCode) {
        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            throw TransferServiceException.invalidCurrency(currencyCode);
        }
    }

    /**
     * Currency validation for wallet services
     */
    public static void validateCurrencyForWallet(String currencyCode) {
        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            throw WalletServiceException.invalidCurrency(currencyCode);
        }
    }

    /**
     * Currency Conformity Check
     */
    public static void assertSameCurrency(String curr1, String curr2) {
        if (!curr1.equals(curr2)) {
            throw TransferServiceException.currencyMismatch(curr1, curr2);
        }
    }

    /**
     * Safe folding
     */
    public static BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.add(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Safe subtraction
     */
    public static BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.subtract(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Formatting the amount for display
     */
    public static String format(BigDecimal amount, String currency) {
        return String.format("%s %s", amount.setScale(2, DEFAULT_ROUNDING).toString(), currency);
    }
}