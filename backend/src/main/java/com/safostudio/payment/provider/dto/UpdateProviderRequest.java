package com.safostudio.payment.provider.dto;

import com.safostudio.payment.provider.domain.ProviderType;

public record UpdateProviderRequest(
        String name,
        String adapterKey,
        ProviderType type,
        String config
) {
}