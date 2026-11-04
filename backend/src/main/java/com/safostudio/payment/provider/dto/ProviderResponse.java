package com.safostudio.payment.provider.dto;

import com.safostudio.payment.provider.domain.Provider;
import com.safostudio.payment.provider.domain.ProviderCapability;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProviderResponse(
        UUID id,
        String code,
        String name,
        String adapterKey,
        String status,
        String type,
        String config,
        Instant lastCheckedAt,
        Instant createdAt,
        Instant updatedAt,
        List<ProviderCapabilityResponse> capabilities,
        boolean hasCredentials
) {
  public static ProviderResponse fromDomain(Provider provider,
                                            List<ProviderCapability> capabilities,
                                            boolean hasCredentials) {
    return new ProviderResponse(
            provider.getId(),
            provider.getCode(),
            provider.getName(),
            provider.getAdapterKey(),
            provider.getStatus().name(),
            provider.getType().name(),
            provider.getConfig(),
            provider.getLastCheckedAt(),
            provider.getCreatedAt(),
            provider.getUpdatedAt(),
            capabilities.stream().map(ProviderCapabilityResponse::fromDomain).toList(),
            hasCredentials
    );
  }
}