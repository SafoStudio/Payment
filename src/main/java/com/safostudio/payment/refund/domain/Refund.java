package com.safostudio.payment.refund.domain;

import com.safostudio.payment.refund.exception.RefundException;
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

    public enum RefundStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REJECTED
    }

    /**
     * Creates a new refund
     */
    public static Refund create(UUID paymentId, BigDecimal amount,
                                String currency, String reason,
                                UUID originalTransactionId) {
        validateAmount(amount);

        return Refund.builder()
                .id(UUID.randomUUID())
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

    /**
     * Processes the refund
     */
    public void process() {
        if (status != RefundStatus.PENDING) {
            throw RefundException.invalidStatus("process", status);
        }
        this.status = RefundStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    /**
     * Completes the refund
     */
    public void complete(UUID refundTransactionId) {
        if (status != RefundStatus.PROCESSING) {
            throw RefundException.invalidStatus("complete", status);
        }
        this.status = RefundStatus.COMPLETED;
        this.refundTransactionId = refundTransactionId;
        this.updatedAt = Instant.now();
    }

    /**
     * Fails the refund
     */
    public void fail(String error) {
        if (status == RefundStatus.COMPLETED) {
            throw RefundException.alreadyCompleted();
        }
        this.status = RefundStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    /**
     * Rejects the refund
     */
    public void reject(String reason) {
        if (status != RefundStatus.PENDING) {
            throw RefundException.cannotReject(status);
        }
        this.status = RefundStatus.REJECTED;
        this.updatedAt = Instant.now();
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw RefundException.amountMustBePositive();
        }
    }

    public boolean isCompleted() {
        return status == RefundStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == RefundStatus.FAILED;
    }
}