package com.safostudio.payment.refund.api.input;

import com.safostudio.payment.refund.domain.RefundStatus;

public record RefundFilterInput(
        RefundStatus status,
        String currency
) {}