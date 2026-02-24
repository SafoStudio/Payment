package com.safostudio.payment.payment.service.dto;

import com.safostudio.payment.payment.domain.PaymentType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class CreatePaymentRequest {
    UUID walletId;
    UUID targetWalletId;
    BigDecimal amount;
    String currency;
    PaymentType type;
    String idempotencyKey;
    String description;
    UUID userId; // for audit
}