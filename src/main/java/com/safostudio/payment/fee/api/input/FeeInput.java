package com.safostudio.payment.fee.api.input;

import java.math.BigDecimal;
import java.util.UUID;

public record FeeInput(
        UUID transactionId,
        UUID fromWalletId,
        UUID toWalletId,
        BigDecimal amount,
        String currency,
        String type,
        BigDecimal percentage,
        String calculationRule
) {}