package com.safostudio.payment.ledger.idempotency.domain;

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
@Table("idempotency_keys")
public class IdempotencyKey {

  @Id
  private UUID id;

  private String operation;

  private String idempotencyKey;

  private String response;

  private String requestFingerprint;

  private IdempotencyStatus status;

  private Instant expiresAt;

  private Instant createdAt;

  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }
}