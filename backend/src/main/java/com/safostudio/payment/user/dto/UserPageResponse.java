package com.safostudio.payment.user.dto;

import java.util.List;

public record UserPageResponse(
        List<UserResponse> items,
        String nextCursor,
        boolean hasMore
) {}
