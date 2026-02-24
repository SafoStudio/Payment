package com.safostudio.payment.payment.controller.input;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentInput(
        UUID walletId,
        UUID targetWalletId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String description
) {}