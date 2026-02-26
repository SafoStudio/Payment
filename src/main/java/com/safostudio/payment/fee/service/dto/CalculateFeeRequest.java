package com.safostudio.payment.fee.service.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class CalculateFeeRequest {
    String transactionType;
    BigDecimal amount;
    String currency;
    UUID fromWalletId;
    UUID toWalletId;
}