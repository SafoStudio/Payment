package com.safostudio.payment.provider.dto;

import com.safostudio.payment.provider.domain.ProviderCredential;

import java.time.Instant;
import java.util.UUID;

public record ProviderCredentialKeyResponse(
        UUID id,
        String credentialKey,
        Instant createdAt,
        Instant rotatedAt
) {
  public static ProviderCredentialKeyResponse fromDomain(ProviderCredential credential) {
    return new ProviderCredentialKeyResponse(
            credential.getId(),
            credential.getCredentialKey(),
            credential.getCreatedAt(),
            credential.getRotatedAt()
    );
  }
}