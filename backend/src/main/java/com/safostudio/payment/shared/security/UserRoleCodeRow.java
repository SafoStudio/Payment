package com.safostudio.payment.shared.security;

import java.util.UUID;

public record UserRoleCodeRow(UUID userId, String code) {
}