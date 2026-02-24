package com.safostudio.payment.payment.service.dto;

import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.domain.PaymentStatus;
import com.safostudio.payment.payment.domain.PaymentType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class PaymentResponse {
    UUID id;
    UUID walletId;
    UUID targetWalletId;
    BigDecimal amount;
    String currency;
    PaymentStatus status;
    PaymentType type;
    String idempotencyKey;
    String description;
    UUID transactionId;
    Instant createdAt;
    Instant updatedAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .walletId(payment.getWalletId())
                .targetWalletId(payment.getTargetWalletId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .type(payment.getType())
                .idempotencyKey(payment.getIdempotencyKey())
                .description(payment.getDescription())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}