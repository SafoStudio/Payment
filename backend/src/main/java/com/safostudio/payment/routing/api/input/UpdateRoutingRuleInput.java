package com.safostudio.payment.routing.api.input;

public record UpdateRoutingRuleInput(
        int priority,
        int weight,
        boolean enabled
) {
}