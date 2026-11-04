package com.safostudio.payment.user.auth.dto;

import java.time.Instant;

public record PasskeyView(
        String credentialId,
        String label,
        Instant created,
        Instant lastUsed
) {}
