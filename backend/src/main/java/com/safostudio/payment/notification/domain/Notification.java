package com.safostudio.payment.notification.domain;

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
@Table("notifications")
public class Notification {

  @Id
  private UUID id;

  private UUID walletId;

  private UUID userId;

  private NotificationType type;

  private String title;

  private String content;

  private NotificationStatus status;

  private Integer retryCount;

  private Instant scheduledFor;

  private Instant sentAt;

  private UUID outboxEventId;

  private Instant createdAt;

  private Instant updatedAt;

  public enum NotificationType {
    PAYMENT_RECEIVED,
    PAYMENT_SENT,
    PAYMENT_FAILED,
    WALLET_BLOCKED,
    WALLET_UNBLOCKED,
    LOW_BALANCE,
    REFUND_PROCESSED,
    REFUND_FAILED
  }

  public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    CANCELLED
  }

  public static Notification create(UUID walletId, UUID userId,
                                    NotificationType type,
                                    String title, String content,
                                    UUID outboxEventId) {
    return Notification.builder()
            .id(null)
            .walletId(walletId)
            .userId(userId)
            .type(type)
            .title(title)
            .content(content)
            .status(NotificationStatus.PENDING)
            .retryCount(0)
            .outboxEventId(outboxEventId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  public void markAsSent() {
    this.status = NotificationStatus.SENT;
    this.sentAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public void markAsFailed() {
    this.status = NotificationStatus.FAILED;
    this.updatedAt = Instant.now();
  }

  public void incrementRetry() {
    this.retryCount++;
    this.updatedAt = Instant.now();
  }

  public boolean shouldRetry(int maxRetries) {
    return retryCount < maxRetries && status == NotificationStatus.FAILED;
  }
}