package com.safostudio.payment.ledger.transaction.dto;

import java.util.List;

public record TransactionPageResponse(
        List<TransactionResponse> items,
        String nextCursor,
        boolean hasMore
) {}