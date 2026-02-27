package com.safostudio.payment.user.controller.input;

public record UpdateUserInput(
        String email,
        String phone,
        String firstName,
        String lastName
) {}