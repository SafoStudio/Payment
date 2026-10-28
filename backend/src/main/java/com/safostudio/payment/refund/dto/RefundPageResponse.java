package com.safostudio.payment.refund.dto;

import java.util.List;

public record RefundPageResponse(
        List<RefundResponse> items,
        String nextCursor,
        boolean hasMore
) {}