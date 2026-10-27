package com.safostudio.payment.payment.api.input;

import com.safostudio.payment.payment.domain.PaymentStatus;
import com.safostudio.payment.payment.domain.PaymentType;

public record PaymentFilterInput(
        PaymentStatus status,
        PaymentType type,
        String currency
) {}