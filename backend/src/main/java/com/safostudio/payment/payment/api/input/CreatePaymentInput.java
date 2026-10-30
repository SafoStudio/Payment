package com.safostudio.payment.payment.api.input;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentInput(
        UUID targetWalletId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String description
) {}