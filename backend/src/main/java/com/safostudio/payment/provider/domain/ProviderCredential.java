package com.safostudio.payment.provider.domain;

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
@Table("provider_credentials")
public class ProviderCredential {

  @Id
  private UUID id;

  private UUID providerId;

  private String credentialKey;

  private String encryptedValue;

  private Instant createdAt;

  private Instant rotatedAt;

  public static ProviderCredential create(UUID providerId, String credentialKey, String encryptedValue) {
    validateCredentialKey(credentialKey);
    validateEncryptedValue(encryptedValue);

    return ProviderCredential.builder()
            .providerId(providerId)
            .credentialKey(credentialKey)
            .encryptedValue(encryptedValue)
            .createdAt(Instant.now())
            .build();
  }

  public void rotate(String encryptedValue) {
    validateEncryptedValue(encryptedValue);
    this.encryptedValue = encryptedValue;
    this.rotatedAt = Instant.now();
  }

  private static void validateCredentialKey(String credentialKey) {
    if (credentialKey == null || credentialKey.isBlank() || credentialKey.length() > 100) {
      throw BusinessException.validationError("Invalid credential key");
    }
  }

  private static void validateEncryptedValue(String encryptedValue) {
    if (encryptedValue == null || encryptedValue.isBlank()) {
      throw BusinessException.validationError("Invalid encrypted value");
    }
  }
}