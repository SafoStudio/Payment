package com.safostudio.payment.routing.dto;

import com.safostudio.payment.provider.domain.Provider;
import com.safostudio.payment.routing.domain.RoutingRule;

import java.util.UUID;

public record ResolvedProvider(
        UUID ruleId,
        UUID providerId,
        String providerCode,
        String providerName,
        String adapterKey,
        int priority
) {
  public static ResolvedProvider from(RoutingRule rule, Provider provider) {
    return new ResolvedProvider(
            rule.getId(),
            provider.getId(),
            provider.getCode(),
            provider.getName(),
            provider.getAdapterKey(),
            rule.getPriority()
    );
  }
}