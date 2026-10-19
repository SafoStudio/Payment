package com.safostudio.payment.user.api.input;

public record UpdateUserInput(
        String email,
        String phone,
        String firstName,
        String lastName
) {}