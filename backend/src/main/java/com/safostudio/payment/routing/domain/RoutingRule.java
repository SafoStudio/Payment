package com.safostudio.payment.routing.domain;

import com.safostudio.payment.shared.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("routing_rules")
public class RoutingRule {

  @Id
  private UUID id;

  private String method;

  private String currency;

  private UUID providerId;

  private int priority;

  private int weight;

  private boolean enabled;

  private Instant createdAt;

  private Instant updatedAt;

  public static RoutingRule create(String method, String currency, UUID providerId,
                                   int priority, int weight) {
    validateMethod(method);
    validateCurrency(currency);
    validateProviderId(providerId);
    validatePriority(priority);
    validateWeight(weight);

    return RoutingRule.builder()
            .method(method)
            .currency(currency)
            .providerId(providerId)
            .priority(priority)
            .weight(weight)
            .enabled(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  public void update(int priority, int weight, boolean enabled) {
    validatePriority(priority);
    validateWeight(weight);

    this.priority = priority;
    this.weight = weight;
    this.enabled = enabled;
    this.updatedAt = Instant.now();
  }

  public void enable() {
    if (enabled) {
      return;
    }
    this.enabled = true;
    this.updatedAt = Instant.now();
  }

  public void disable() {
    if (!enabled) {
      return;
    }
    this.enabled = false;
    this.updatedAt = Instant.now();
  }

  public boolean matches(String method, String currency) {
    return enabled
            && this.method.equals(method)
            && this.currency.equals(currency);
  }

  private static void validateMethod(String method) {
    if (method == null || !method.matches("CARD|BANK_TRANSFER|WALLET")) {
      throw BusinessException.validationError("Invalid payment method: " + method);
    }
  }

  private static void validateCurrency(String currency) {
    if (currency == null || !currency.matches("^[A-Z]{3}$")) {
      throw BusinessException.invalidCurrency(currency);
    }
  }

  private static void validateProviderId(UUID providerId) {
    if (providerId == null) {
      throw BusinessException.validationError("Provider id is required");
    }
  }

  private static void validatePriority(int priority) {
    if (priority < 0) {
      throw BusinessException.invalidPriority(priority);
    }
  }

  private static void validateWeight(int weight) {
    if (weight <= 0) {
      throw BusinessException.invalidWeight(weight);
    }
  }
}