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
@Table("providers")
public class Provider {

  @Id
  private UUID id;

  private String code;

  private String name;

  private String adapterKey;

  private ProviderStatus status;

  private ProviderType type;

  private String config;

  private Instant lastCheckedAt;

  private Instant createdAt;

  private Instant updatedAt;

  public static Provider create(String code, String name, String adapterKey,
                                ProviderType type, String config) {
    validateCode(code);
    validateName(name);
    validateAdapterKey(adapterKey);
    validateType(type);

    return Provider.builder()
            .code(code)
            .name(name)
            .adapterKey(adapterKey)
            .status(ProviderStatus.DISABLED)
            .type(type)
            .config(config)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  public void update(String name, String adapterKey, ProviderType type, String config) {
    validateName(name);
    validateAdapterKey(adapterKey);
    validateType(type);

    this.name = name;
    this.adapterKey = adapterKey;
    this.type = type;
    this.config = config;
    this.updatedAt = Instant.now();
  }

  public void activate() {
    if (status == ProviderStatus.ACTIVE) {
      return;
    }
    this.status = ProviderStatus.ACTIVE;
    this.updatedAt = Instant.now();
  }

  public void degrade() {
    if (status == ProviderStatus.DEGRADED) {
      return;
    }
    this.status = ProviderStatus.DEGRADED;
    this.updatedAt = Instant.now();
  }

  public void disable() {
    if (status == ProviderStatus.DISABLED) {
      return;
    }
    this.status = ProviderStatus.DISABLED;
    this.updatedAt = Instant.now();
  }

  public void markChecked(Instant checkedAt) {
    this.lastCheckedAt = checkedAt;
    this.updatedAt = Instant.now();
  }

  public boolean isActive() {
    return status == ProviderStatus.ACTIVE;
  }

  private static void validateCode(String code) {
    if (code == null || code.isBlank() || code.length() > 50) {
      throw BusinessException.validationError("Invalid provider code");
    }
  }

  private static void validateName(String name) {
    if (name == null || name.isBlank() || name.length() > 100) {
      throw BusinessException.validationError("Invalid provider name");
    }
  }

  private static void validateAdapterKey(String adapterKey) {
    if (adapterKey == null || adapterKey.isBlank() || adapterKey.length() > 50) {
      throw BusinessException.validationError("Invalid adapter key");
    }
  }

  private static void validateType(ProviderType type) {
    if (type == null) {
      throw BusinessException.validationError("Provider type is required");
    }
  }
}