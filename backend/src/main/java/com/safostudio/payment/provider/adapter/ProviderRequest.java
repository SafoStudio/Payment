package com.safostudio.payment.provider.adapter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ProviderRequest(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String method,
        String config,
        Map<String, String> metadata,
        Map<String, String> credentials
) {
  public ProviderRequest {
    if (paymentId == null) {
      throw new IllegalArgumentException("paymentId is required");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amount must be positive");
    }
    if (currency == null || !currency.matches("^[A-Z]{3}$")) {
      throw new IllegalArgumentException("currency must be ISO-4217");
    }
    if (method == null || method.isBlank()) {
      throw new IllegalArgumentException("method is required");
    }
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    credentials = credentials == null ? Map.of() : Map.copyOf(credentials);
  }
}