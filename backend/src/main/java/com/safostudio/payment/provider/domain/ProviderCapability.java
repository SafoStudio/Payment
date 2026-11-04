package com.safostudio.payment.provider.domain;

import com.safostudio.payment.shared.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("provider_capabilities")
public class ProviderCapability {

  @Id
  private UUID id;

  private UUID providerId;

  private PaymentMethod method;

  private String currency;

  private BigDecimal minAmount;

  private BigDecimal maxAmount;

  private Boolean enabled;

  private Instant createdAt;

  private Instant updatedAt;

  public static ProviderCapability create(UUID providerId, PaymentMethod method, String currency,
                                          BigDecimal minAmount, BigDecimal maxAmount, Boolean enabled) {
    validateMethod(method);
    validateCurrency(currency);
    validateAmounts(minAmount, maxAmount);

    return ProviderCapability.builder()
            .providerId(providerId)
            .method(method)
            .currency(currency)
            .minAmount(minAmount)
            .maxAmount(maxAmount)
            .enabled(enabled == null || enabled)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  public void update(PaymentMethod method, String currency,
                     BigDecimal minAmount, BigDecimal maxAmount, Boolean enabled) {
    validateMethod(method);
    validateCurrency(currency);
    validateAmounts(minAmount, maxAmount);

    this.method = method;
    this.currency = currency;
    this.minAmount = minAmount;
    this.maxAmount = maxAmount;
    this.enabled = enabled == null || enabled;
    this.updatedAt = Instant.now();
  }

  public boolean supports(BigDecimal amount) {
    return Boolean.TRUE.equals(enabled)
            && amount != null
            && amount.compareTo(minAmount) >= 0
            && amount.compareTo(maxAmount) <= 0;
  }

  private static void validateMethod(PaymentMethod method) {
    if (method == null) {
      throw BusinessException.validationError("Payment method is required");
    }
  }

  private static void validateCurrency(String currency) {
    if (currency == null || !currency.matches("^[A-Z]{3}$")) {
      throw BusinessException.invalidCurrency(currency);
    }
  }

  private static void validateAmounts(BigDecimal minAmount, BigDecimal maxAmount) {
    if (minAmount == null || minAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw BusinessException.validationError("Invalid min amount");
    }
    if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw BusinessException.validationError("Invalid max amount");
    }
    if (minAmount.compareTo(maxAmount) > 0) {
      throw BusinessException.validationError("min amount must be <= max amount");
    }
  }
}