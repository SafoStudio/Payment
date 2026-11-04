package com.safostudio.payment.provider.api.input;

public record SetProviderCredentialInput(
        String credentialKey,
        String value
) {
}