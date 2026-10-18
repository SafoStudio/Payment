package com.safostudio.payment.user.api.input;

import com.safostudio.payment.user.domain.User;

public record CreateUserInput(
        String email,
        String phone,
        String firstName,
        String lastName,
        User.UserRole role
) {}