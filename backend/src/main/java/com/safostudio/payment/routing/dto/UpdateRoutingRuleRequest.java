package com.safostudio.payment.routing.dto;

public record UpdateRoutingRuleRequest(
        int priority,
        int weight,
        boolean enabled
) {
}