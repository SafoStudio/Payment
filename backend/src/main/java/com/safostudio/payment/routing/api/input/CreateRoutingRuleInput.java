package com.safostudio.payment.routing.api.input;

import com.safostudio.payment.provider.domain.PaymentMethod;

import java.util.UUID;

public record CreateRoutingRuleInput(
        PaymentMethod method,
        String currency,
        UUID providerId,
        int priority,
        int weight
) {
}