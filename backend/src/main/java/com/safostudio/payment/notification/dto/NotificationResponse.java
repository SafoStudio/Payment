package com.safostudio.payment.notification.dto;

import com.safostudio.payment.notification.domain.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID walletId,
        UUID userId,
        String type,
        String title,
        String content,
        String status,
        int retryCount,
        Instant scheduledFor,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt
) {
  public static NotificationResponse fromDomain(Notification n) {
    return new NotificationResponse(
            n.getId(),
            n.getWalletId(),
            n.getUserId(),
            n.getType().name(),
            n.getTitle(),
            n.getContent(),
            n.getStatus().name(),
            n.getRetryCount() == null ? 0 : n.getRetryCount(),
            n.getScheduledFor(),
            n.getSentAt(),
            n.getCreatedAt(),
            n.getUpdatedAt()
    );
  }
}