package com.safostudio.payment.fee.controller.input;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateFeeRuleInput(
        String name,
        String transactionType,
        String feeType,
        BigDecimal fixedAmount,
        BigDecimal percentage,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String currency,
        UUID fromWalletId,
        UUID toWalletId,
        Boolean isActive
) {}