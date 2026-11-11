package com.safostudio.payment.payment.api.input;

import com.safostudio.payment.provider.domain.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentInput(
        UUID targetWalletId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        String idempotencyKey,
        String description
) {}