package com.safostudio.payment.payment.domain;

import com.safostudio.payment.shared.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payments")
public class Payment {

  @Id
  private UUID id;

  private UUID walletId;

  private UUID targetWalletId;

  private BigDecimal amount;

  private String currency;

  private String method;

  private PaymentStatus status;

  private PaymentType type;

  private String idempotencyKey;

  private String description;

  private UUID transactionId;

  private UUID providerId;

  private String providerReference;

  private String failureReason;

  private String metadata;

  private Instant createdAt;

  private Instant updatedAt;

  public static Payment create(NewPayment input) {
    validateAmount(input.amount());
    validateMethod(input.method());
    validateWallets(input.walletId(), input.targetWalletId(), input.type());

    return Payment.builder()
            .id(null)
            .walletId(input.walletId())
            .targetWalletId(input.targetWalletId())
            .amount(input.amount())
            .currency(input.currency())
            .method(input.method())
            .status(PaymentStatus.PENDING)
            .type(input.type())
            .idempotencyKey(input.idempotencyKey())
            .description(input.description())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  public void markAsProcessing() {
    if (status != PaymentStatus.PENDING) {
      throw BusinessException.invalidPaymentStatus("process", status);
    }
    this.status = PaymentStatus.PROCESSING;
    this.updatedAt = Instant.now();
  }

  public void succeed(UUID transactionId, UUID providerId, String providerReference) {
    if (status != PaymentStatus.PROCESSING && status != PaymentStatus.PENDING) {
      throw BusinessException.invalidPaymentStatus("complete", status);
    }
    this.status = PaymentStatus.SUCCEEDED;
    this.transactionId = transactionId;
    this.providerId = providerId;
    this.providerReference = providerReference;
    this.failureReason = null;
    this.updatedAt = Instant.now();
  }

  public void fail(UUID providerId, String failureReason) {
    if (status == PaymentStatus.SUCCEEDED) {
      throw BusinessException.paymentAlreadySucceeded();
    }
    this.status = PaymentStatus.FAILED;
    this.providerId = providerId;
    this.failureReason = failureReason;
    this.updatedAt = Instant.now();
  }

  public void refund() {
    if (status != PaymentStatus.SUCCEEDED) {
      throw BusinessException.paymentCannotRefund(status);
    }
    this.status = PaymentStatus.REFUNDED;
    this.updatedAt = Instant.now();
  }

  public void cancel() {
    if (status != PaymentStatus.PENDING) {
      throw BusinessException.paymentCannotCancel(status);
    }
    this.status = PaymentStatus.CANCELLED;
    this.updatedAt = Instant.now();
  }

  public boolean isPending() {
    return status == PaymentStatus.PENDING;
  }
  public boolean isSucceeded() {
    return status == PaymentStatus.SUCCEEDED;
  }
  public boolean isFailed() {
    return status == PaymentStatus.FAILED;
  }

  private static void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw BusinessException.invalidAmount();
    }
  }

  private static void validateMethod(String method) {
    if (method == null || !method.matches("CARD|BANK_TRANSFER|WALLET")) {
      throw BusinessException.validationError("Invalid payment method: " + method);
    }
  }

  private static void validateWallets(UUID walletId, UUID targetWalletId, PaymentType type) {
    if (type != PaymentType.TRANSFER && type != PaymentType.PAYMENT) {
      return;
    }
    if (walletId == null || targetWalletId == null) {
      throw BusinessException.validationError("Wallet IDs must not be null for transfer/payment");
    }
    if (walletId.equals(targetWalletId)) {
      throw BusinessException.sameWallet();
    }
  }
}