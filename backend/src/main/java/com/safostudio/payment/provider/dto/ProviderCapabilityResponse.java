package com.safostudio.payment.provider.dto;

import com.safostudio.payment.provider.domain.ProviderCapability;

import java.time.Instant;
import java.util.UUID;

public record ProviderCapabilityResponse(
        UUID id,
        String method,
        String currency,
        String minAmount,
        String maxAmount,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
  public static ProviderCapabilityResponse fromDomain(ProviderCapability capability) {
    return new ProviderCapabilityResponse(
            capability.getId(),
            capability.getMethod().name(),
            capability.getCurrency(),
            capability.getMinAmount().toPlainString(),
            capability.getMaxAmount().toPlainString(),
            Boolean.TRUE.equals(capability.getEnabled()),
            capability.getCreatedAt(),
            capability.getUpdatedAt()
    );
  }
}