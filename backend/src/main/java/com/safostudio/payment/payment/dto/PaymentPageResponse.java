package com.safostudio.payment.payment.dto;

import java.util.List;

public record PaymentPageResponse(
        List<PaymentResponse> items,
        String nextCursor,
        boolean hasMore
) {}