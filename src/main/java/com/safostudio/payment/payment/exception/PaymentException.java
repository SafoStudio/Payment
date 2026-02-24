package com.safostudio.payment.payment.exception;

import com.safostudio.payment.payment.domain.PaymentStatus;

import java.util.UUID;

public class PaymentException extends RuntimeException {

    private PaymentException(String message) {
        super(message);
    }

    public static PaymentException invalidStatus(String action, PaymentStatus current) {
        return new PaymentException(
                "Cannot %s payment. Current status: %s".formatted(action, current)
        );
    }

    public static PaymentException alreadySucceeded() {
        return new PaymentException("Payment already succeeded");
    }

    public static PaymentException cannotRefund(PaymentStatus status) {
        return new PaymentException("Cannot refund payment with status: %s".formatted(status));
    }

    public static PaymentException cannotCancel(PaymentStatus status) {
        return new PaymentException("Cannot cancel payment with status: %s".formatted(status));
    }

    public static PaymentException amountMustBePositive() {
        return new PaymentException("Amount must be positive");
    }

    public static PaymentException sameWallet() {
        return new PaymentException("Source and target wallets must be different");
    }

    public static PaymentException notFound(UUID id) {
        return new PaymentException("Payment not found: %s".formatted(id));
    }

    public static PaymentException idempotencyKeyExists(String key) {
        return new PaymentException("Payment with idempotency key already exists: %s".formatted(key));
    }
}