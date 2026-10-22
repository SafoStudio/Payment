package com.safostudio.payment.shared.util;

import com.safostudio.payment.shared.exception.BusinessException;

import java.math.BigDecimal;
import java.util.Currency;

public final class MoneyUtils {

  private MoneyUtils() {}

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
}