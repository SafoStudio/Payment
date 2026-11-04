package com.safostudio.payment.provider.dto;

import com.safostudio.payment.provider.domain.PaymentMethod;

import java.math.BigDecimal;

public record UpdateCapabilityRequest(
        PaymentMethod method,
        String currency,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Boolean enabled
) {
}