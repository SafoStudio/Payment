package com.safostudio.payment.notification.dto;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationResponse> items,
        String nextCursor,
        boolean hasMore
) {
}