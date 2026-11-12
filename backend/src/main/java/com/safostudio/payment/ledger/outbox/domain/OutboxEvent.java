package com.safostudio.payment.ledger.outbox.domain;

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
@Table("outbox_events")
public class OutboxEvent {

  @Id
  private UUID id;

  private UUID eventId;

  private String aggregateType;

  private UUID aggregateId;

  private String eventType;

  private String payload;

  private OutboxStatus status;

  private int attempts;

  private Instant nextAttemptAt;

  private String lastError;

  private Instant createdAt;

  private Instant processedAt;

  public enum OutboxStatus {
    PENDING,
    PROCESSED,
    FAILED,
    DEAD
  }

  public static OutboxEvent pending(UUID eventId, String aggregateType, UUID aggregateId,
                                    String eventType, String payload) {
    return OutboxEvent.builder()
            .id(null)
            .eventId(eventId)
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(eventType)
            .payload(payload)
            .status(OutboxStatus.PENDING)
            .attempts(0)
            .nextAttemptAt(Instant.now())
            .createdAt(Instant.now())
            .build();
  }

  public void markProcessed() {
    this.status = OutboxStatus.PROCESSED;
    this.processedAt = Instant.now();
    this.lastError = null;
  }

  public void markFailed(String error, Instant nextAttemptAt) {
    this.status = OutboxStatus.FAILED;
    this.attempts++;
    this.lastError = error;
    this.nextAttemptAt = nextAttemptAt;
  }

  public void markDead(String error) {
    this.status = OutboxStatus.DEAD;
    this.attempts++;
    this.lastError = error;
  }

  public boolean isPendingOrFailed() {
    return status == OutboxStatus.PENDING || status == OutboxStatus.FAILED;
  }
}