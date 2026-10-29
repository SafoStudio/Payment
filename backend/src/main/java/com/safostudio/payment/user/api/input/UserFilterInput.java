package com.safostudio.payment.user.api.input;

import com.safostudio.payment.user.domain.UserStatus;

public record UserFilterInput(
        UserStatus status,
        String roleCode
) {}
