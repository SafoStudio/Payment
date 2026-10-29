package com.safostudio.payment.ledger.balance.dto;

import java.util.List;

public record BalanceHistoryPageResponse(
        List<BalanceHistoryResponse> items,
        String nextCursor,
        boolean hasMore
) {}
