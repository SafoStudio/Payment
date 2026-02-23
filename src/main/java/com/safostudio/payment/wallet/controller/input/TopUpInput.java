package com.safostudio.payment.wallet.controller.input;

import java.math.BigDecimal;
import java.util.UUID;

public record TopUpInput(
        UUID walletId,
        BigDecimal amount,
        String currency,
        String description,
        String idempotencyKey
) {}