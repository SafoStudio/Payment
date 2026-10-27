package com.safostudio.payment.ledger.transaction.api.input;

import java.math.BigDecimal;
import java.util.UUID;

public record TopUpInput(
        UUID walletId,
        BigDecimal amount,
        String currency,
        String description,
        String idempotencyKey
) {}