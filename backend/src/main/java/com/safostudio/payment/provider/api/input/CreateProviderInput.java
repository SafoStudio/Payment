package com.safostudio.payment.provider.api.input;

import com.safostudio.payment.provider.domain.ProviderType;

public record CreateProviderInput(
        String code,
        String name,
        String adapterKey,
        ProviderType type,
        String config
) {
}