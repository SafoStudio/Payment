package com.safostudio.payment.refund.domain;

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
@Table("refunds")
public class Refund {

  @Id
  private UUID id;

  private UUID paymentId;

  private UUID transactionId;

  private BigDecimal amount;

  private String currency;

  private RefundStatus status;

  private String reason;

  private UUID originalTransactionId;

  private UUID refundTransactionId;

  private Instant createdAt;

  private Instant updatedAt;

  public static Refund create(UUID paymentId, BigDecimal amount,
                              String currency, String reason,
                              UUID originalTransactionId) {

    validateAmount(amount);

    return Refund.builder()
            .id(null)
            .paymentId(paymentId)
            .amount(amount)
            .currency(currency)
            .status(RefundStatus.PENDING)
            .reason(reason)
            .originalTransactionId(originalTransactionId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  public void process() {
    if (status != RefundStatus.PENDING) {
      throw BusinessException.invalidRefundStatus("process", status);
    }
    this.status = RefundStatus.PROCESSING;
  }

  public void complete(UUID refundTransactionId) {
    if (status != RefundStatus.PROCESSING) {
      throw BusinessException.invalidRefundStatus("complete", status);
    }
    this.status = RefundStatus.COMPLETED;
    this.refundTransactionId = refundTransactionId;
  }

  public void fail() {
    if (status == RefundStatus.COMPLETED) {
      throw BusinessException.refundAlreadyCompleted();
    }
    this.status = RefundStatus.FAILED;
  }

  public void reject() {
    if (status != RefundStatus.PENDING) {
      throw BusinessException.refundCannotReject(status);
    }
    this.status = RefundStatus.REJECTED;
  }

  private static void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw BusinessException.invalidAmount();
    }
  }

  public boolean isCompleted() {
    return status == RefundStatus.COMPLETED;
  }

  public boolean isFailed() {
    return status == RefundStatus.FAILED;
  }
}