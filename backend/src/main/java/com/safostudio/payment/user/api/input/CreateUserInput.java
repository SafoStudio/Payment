package com.safostudio.payment.user.api.input;

import java.util.List;

public record CreateUserInput(
        String email,
        String phone,
        String firstName,
        String lastName,
        List<String> roleCodes
) {}