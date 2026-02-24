package com.safostudio.payment.payment.domain;

import com.safostudio.payment.payment.exception.PaymentException;
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

    /**
     * Creates a new payment
     */
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

    /**
     * Marks payment as processing
     */
    public void markAsProcessing() {
        if (status != PaymentStatus.PENDING) {
            throw PaymentException.invalidStatus("process", status);
        }
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks payment as succeeded
     */
    public void succeed(UUID transactionId) {
        if (status != PaymentStatus.PROCESSING && status != PaymentStatus.PENDING) {
            throw PaymentException.invalidStatus("complete", status);
        }
        this.status = PaymentStatus.SUCCEEDED;
        this.transactionId = transactionId;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks payment as failed
     */
    public void fail() {
        if (status == PaymentStatus.SUCCEEDED) {
            throw PaymentException.alreadySucceeded();
        }
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks payment as refunded
     */
    public void refund() {
        if (status != PaymentStatus.SUCCEEDED) {
            throw PaymentException.cannotRefund(status);
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    /**
     * Cancels payment
     */
    public void cancel() {
        if (status != PaymentStatus.PENDING) {
            throw PaymentException.cannotCancel(status);
        }
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Validations
     */
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
            throw PaymentException.amountMustBePositive();
        }
    }

    private static void validateWallets(UUID walletId, UUID targetWalletId, PaymentType type) {
        if (type == PaymentType.TRANSFER || type == PaymentType.PAYMENT) {
            if (walletId.equals(targetWalletId)) {
                throw PaymentException.sameWallet();
            }
        }
    }
}