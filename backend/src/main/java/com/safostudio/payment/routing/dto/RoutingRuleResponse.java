package com.safostudio.payment.routing.dto;

import com.safostudio.payment.provider.domain.Provider;
import com.safostudio.payment.routing.domain.RoutingRule;

import java.time.Instant;
import java.util.UUID;

public record RoutingRuleResponse(
        UUID id,
        String method,
        String currency,
        UUID providerId,
        String providerCode,
        String providerName,
        int priority,
        int weight,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
  public static RoutingRuleResponse fromDomain(RoutingRule rule, Provider provider) {
    return new RoutingRuleResponse(
            rule.getId(),
            rule.getMethod(),
            rule.getCurrency(),
            rule.getProviderId(),
            provider != null ? provider.getCode() : null,
            provider != null ? provider.getName() : null,
            rule.getPriority(),
            rule.getWeight(),
            rule.isEnabled(),
            rule.getCreatedAt(),
            rule.getUpdatedAt()
    );
  }
}