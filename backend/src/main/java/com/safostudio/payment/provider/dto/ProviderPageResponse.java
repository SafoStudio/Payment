package com.safostudio.payment.provider.dto;

import java.util.List;

public record ProviderPageResponse(
        List<ProviderResponse> items,
        String nextCursor,
        boolean hasMore
) {
}