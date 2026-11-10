package com.safostudio.payment.routing.dto;

import com.safostudio.payment.provider.domain.PaymentMethod;

import java.util.UUID;

public record CreateRoutingRuleRequest(
        PaymentMethod method,
        String currency,
        UUID providerId,
        int priority,
        int weight
) {
}