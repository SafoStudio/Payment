package com.safostudio.payment.ledger.transaction.api.input;

import java.math.BigDecimal;
import java.util.UUID;

public record AdjustBalanceInput(
        UUID walletId,
        BigDecimal amount,
        String reason,
        String idempotencyKey
) {}