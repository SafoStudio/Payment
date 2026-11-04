package com.safostudio.payment.provider.api.input;

public record RotateProviderCredentialInput(
        String credentialKey,
        String value
) {
}