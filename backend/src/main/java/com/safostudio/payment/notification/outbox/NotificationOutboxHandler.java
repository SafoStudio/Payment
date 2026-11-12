package com.safostudio.payment.notification.outbox;

import com.safostudio.payment.ledger.outbox.domain.OutboxEvent;
import com.safostudio.payment.ledger.outbox.handler.OutboxEventHandler;
import com.safostudio.payment.notification.domain.Notification;
import com.safostudio.payment.notification.repository.NotificationRepository;
import com.safostudio.payment.wallet.domain.Wallet;
import com.safostudio.payment.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxHandler implements OutboxEventHandler {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final NotificationRepository notificationRepository;
  private final WalletRepository walletRepository;

  @Override
  public List<String> supportedEventTypes() {
    return List.of("PaymentSucceeded", "PaymentFailed", "RefundCompleted", "RefundFailed");
  }

  @Override
  public void handle(OutboxEvent event) {
    JsonNode payload;
    try {
      payload = MAPPER.readTree(event.getPayload());
    } catch (RuntimeException e) {
      throw new IllegalStateException("Invalid outbox payload for event " + event.getEventId(), e);
    }

    UUID walletId = uuidOrNull(payload);
    UUID userId = resolveUserId(walletId);

    Notification notification = switch (event.getEventType()) {
      case "PaymentSucceeded" -> Notification.create(
              walletId, userId,
              Notification.NotificationType.PAYMENT_RECEIVED,
              "Payment received",
              "Payment %s of %s %s succeeded.".formatted(
                      text(payload, "paymentId"),
                      money(payload),
                      text(payload, "currency")),
              event.getEventId()
      );
      case "PaymentFailed" -> Notification.create(
              walletId, userId,
              Notification.NotificationType.PAYMENT_FAILED,
              "Payment failed",
              "Payment %s of %s %s failed: %s.".formatted(
                      text(payload, "paymentId"),
                      money(payload),
                      text(payload, "currency"),
                      text(payload, "failureReason")),
              event.getEventId()
      );
      case "RefundCompleted" -> Notification.create(
              walletId, userId,
              Notification.NotificationType.REFUND_PROCESSED,
              "Refund processed",
              "Refund %s of %s %s for payment %s completed.".formatted(
                      text(payload, "refundId"),
                      money(payload),
                      text(payload, "currency"),
                      text(payload, "paymentId")),
              event.getEventId()
      );
      case "RefundFailed" -> Notification.create(
              walletId, userId,
              Notification.NotificationType.REFUND_FAILED,
              "Refund failed",
              "Refund %s of %s %s for payment %s failed: %s.".formatted(
                      text(payload, "refundId"),
                      money(payload),
                      text(payload, "currency"),
                      text(payload, "paymentId"),
                      text(payload, "failureReason")),
              event.getEventId()
      );
      default -> null;
    };

    if (notification == null) {
      return;
    }

    try {
      notificationRepository.save(notification);
    } catch (DuplicateKeyException e) {
      log.debug("Notification for outbox event {} already exists, skipping", event.getEventId());
    }
  }

  private UUID resolveUserId(UUID walletId) {
    if (walletId == null) {
      return null;
    }
    return walletRepository.findById(walletId)
            .map(Wallet::getUserId)
            .orElse(null);
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? "—" : value.asString();
  }

  private static String money(JsonNode node) {
    JsonNode value = node.get("amount");
    if (value == null || value.isNull()) {
      return "—";
    }
    try {
      return new BigDecimal(value.asString())
              .setScale(2, RoundingMode.HALF_UP)
              .toPlainString();
    } catch (NumberFormatException e) {
      return value.asString();
    }
  }

  private static UUID uuidOrNull(JsonNode node) {
    JsonNode value = node.get("targetWalletId");
    if (value == null || value.isNull()) {
      return null;
    }
    try {
      return UUID.fromString(value.asString());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}