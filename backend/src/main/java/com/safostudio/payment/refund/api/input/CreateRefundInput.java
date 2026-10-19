package com.safostudio.payment.refund.api.input;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRefundInput(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String reason,
        String idempotencyKey
) {}