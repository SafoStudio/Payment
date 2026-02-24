package com.safostudio.payment.payment.domain;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    REFUNDED,
    CANCELLED
}