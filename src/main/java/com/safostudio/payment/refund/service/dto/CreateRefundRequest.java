package com.safostudio.payment.refund.service.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class CreateRefundRequest {
    UUID paymentId;
    BigDecimal amount;
    String currency;
    String reason;
    String idempotencyKey;
}