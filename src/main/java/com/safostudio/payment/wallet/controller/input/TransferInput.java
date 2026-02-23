package com.safostudio.payment.wallet.controller.input;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferInput(
        UUID fromWalletId,
        UUID toWalletId,
        BigDecimal amount,
        String currency,
        String description,
        String idempotencyKey
) {}