package com.safostudio.payment.shared.util;

import com.safostudio.payment.shared.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public final class MoneyUtils {

  private MoneyUtils() {}

  public static final BigDecimal DEFAULT_AMOUNT = BigDecimal.ZERO;
  public static final int DEFAULT_SCALE = 4;
  public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;

  public static void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw BusinessException.invalidAmount();
    }
  }

  public static void validateCurrency(String currencyCode) {
    try {
      Currency.getInstance(currencyCode);
    } catch (IllegalArgumentException e) {
      throw BusinessException.invalidCurrency(currencyCode);
    }
  }

  public static void assertSameCurrency(String curr1, String curr2) {
    if (!curr1.equals(curr2)) {
      throw BusinessException.currencyMismatch(curr1, curr2);
    }
  }

  public static BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
    if (a == null) a = BigDecimal.ZERO;
    if (b == null) b = BigDecimal.ZERO;
    return a.add(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
  }

  public static BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
    if (a == null) a = BigDecimal.ZERO;
    if (b == null) b = BigDecimal.ZERO;
    return a.subtract(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
  }

  public static String format(BigDecimal amount, String currency) {
    return String.format("%s %s", amount.setScale(2, DEFAULT_ROUNDING).toString(), currency);
  }
}