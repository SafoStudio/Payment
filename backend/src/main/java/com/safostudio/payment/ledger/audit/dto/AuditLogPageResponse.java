package com.safostudio.payment.ledger.audit.dto;

import java.util.List;

public record AuditLogPageResponse(
        List<AuditLogResponse> items,
        String nextCursor,
        boolean hasMore
) {}