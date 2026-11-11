package com.safostudio.payment.payment.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record NewPayment(
        UUID walletId,
        UUID targetWalletId,
        BigDecimal amount,
        String currency,
        String method,
        PaymentType type,
        String idempotencyKey,
        String description
) {
}