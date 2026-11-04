package com.safostudio.payment.user.api.input;

public record UpdateMeInput(
        String phone,
        String firstName,
        String lastName
) {}
