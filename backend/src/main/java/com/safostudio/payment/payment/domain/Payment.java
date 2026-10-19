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

  private PaymentStatus status;

  private PaymentType type;

  private String idempotencyKey;

  private String description;

  private UUID transactionId;

  private Instant createdAt;

  private Instant updatedAt;

  public static Payment create(UUID walletId, UUID targetWalletId,
                               BigDecimal amount, String currency,
                               PaymentType type, String idempotencyKey,
                               String description) {
    validateAmount(amount);
    validateWallets(walletId, targetWalletId, type);

    return Payment.builder()
            .id(null)
            .walletId(walletId)
            .targetWalletId(targetWalletId)
            .amount(amount)
            .currency(currency)
            .status(PaymentStatus.PENDING)
            .type(type)
            .idempotencyKey(idempotencyKey)
            .description(description)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  public void markAsProcessing() {
    if (status != PaymentStatus.PENDING) {
      throw BusinessException.invalidPaymentStatus("process", status);
    }
    this.status = PaymentStatus.PROCESSING;
  }

  public void succeed(UUID transactionId) {
    if (status != PaymentStatus.PROCESSING && status != PaymentStatus.PENDING) {
      throw BusinessException.invalidPaymentStatus("complete", status);
    }
    this.status = PaymentStatus.SUCCEEDED;
    this.transactionId = transactionId;
  }

  public void fail() {
    if (status == PaymentStatus.SUCCEEDED) {
      throw BusinessException.paymentAlreadySucceeded();
    }
    this.status = PaymentStatus.FAILED;
  }

  public void refund() {
    if (status != PaymentStatus.SUCCEEDED) {
      throw BusinessException.paymentCannotRefund(status);
    }
    this.status = PaymentStatus.REFUNDED;
  }

  public void cancel() {
    if (status != PaymentStatus.PENDING) {
      throw BusinessException.paymentCannotCancel(status);
    }
    this.status = PaymentStatus.CANCELLED;
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