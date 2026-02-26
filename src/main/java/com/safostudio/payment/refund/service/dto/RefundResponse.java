package com.safostudio.payment.refund.service.dto;

import com.safostudio.payment.refund.domain.Refund;
import com.safostudio.payment.refund.domain.Refund.RefundStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class RefundResponse {
    UUID id;
    UUID paymentId;
    BigDecimal amount;
    String currency;
    RefundStatus status;
    String reason;
    UUID originalTransactionId;
    UUID refundTransactionId;
    Instant createdAt;
    Instant updatedAt;

    public static RefundResponse from(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .paymentId(refund.getPaymentId())
                .amount(refund.getAmount())
                .currency(refund.getCurrency())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .originalTransactionId(refund.getOriginalTransactionId())
                .refundTransactionId(refund.getRefundTransactionId())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .build();
    }
}