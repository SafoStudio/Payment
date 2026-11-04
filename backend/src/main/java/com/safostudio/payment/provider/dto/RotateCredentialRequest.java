package com.safostudio.payment.provider.dto;

public record RotateCredentialRequest(
        String credentialKey,
        String value
) {
}
