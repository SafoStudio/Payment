package com.safostudio.payment.routing.dto;

import java.util.List;

public record RoutingRulePageResponse(
        List<RoutingRuleResponse> items,
        String nextCursor,
        boolean hasMore
) {
}