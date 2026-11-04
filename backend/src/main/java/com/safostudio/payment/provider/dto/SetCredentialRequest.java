package com.safostudio.payment.provider.dto;

public record SetCredentialRequest(
        String credentialKey,
        String value
) {
}