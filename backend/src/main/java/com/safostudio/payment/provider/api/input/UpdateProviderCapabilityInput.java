package com.safostudio.payment.provider.api.input;

import com.safostudio.payment.provider.domain.PaymentMethod;

import java.math.BigDecimal;

public record UpdateProviderCapabilityInput(
        PaymentMethod method,
        String currency,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Boolean enabled
) {
}