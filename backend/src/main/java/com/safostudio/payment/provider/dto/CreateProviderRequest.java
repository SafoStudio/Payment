package com.safostudio.payment.provider.dto;

import com.safostudio.payment.provider.domain.ProviderType;

public record CreateProviderRequest(
        String code,
        String name,
        String adapterKey,
        ProviderType type,
        String config
) {
}