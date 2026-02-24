package com.safostudio.payment.refund.exception;

import com.safostudio.payment.refund.domain.Refund.RefundStatus;

import java.math.BigDecimal;
import java.util.UUID;

public class RefundException extends RuntimeException {

    private RefundException(String message) {
        super(message);
    }

    public static RefundException invalidStatus(String action, RefundStatus current) {
        return new RefundException(
                "Cannot %s refund. Current status: %s".formatted(action, current)
        );
    }

    public static RefundException alreadyCompleted() {
        return new RefundException("Refund already completed");
    }

    public static RefundException cannotReject(RefundStatus status) {
        return new RefundException("Cannot reject refund with status: %s".formatted(status));
    }

    public static RefundException amountMustBePositive() {
        return new RefundException("Refund amount must be positive");
    }

    public static RefundException notFound(UUID id) {
        return new RefundException("Refund not found: %s".formatted(id));
    }

    public static RefundException exceedsPaymentAmount(BigDecimal refundAmount, BigDecimal paymentAmount) {
        return new RefundException(
                "Refund amount %s exceeds payment amount %s".formatted(refundAmount, paymentAmount)
        );
    }
}