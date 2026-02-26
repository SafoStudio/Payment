package com.safostudio.payment.fee.service.dto;

import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.domain.Fee.FeeType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class FeeResponse {
    UUID id;
    UUID transactionId;
    UUID fromWalletId;
    UUID toWalletId;
    BigDecimal amount;
    String currency;
    FeeType type;
    BigDecimal percentage;
    String calculationRule;
    Instant createdAt;

    public static FeeResponse from(Fee fee) {
        return FeeResponse.builder()
                .id(fee.getId())
                .transactionId(fee.getTransactionId())
                .fromWalletId(fee.getFromWalletId())
                .toWalletId(fee.getToWalletId())
                .amount(fee.getAmount())
                .currency(fee.getCurrency())
                .type(fee.getType())
                .percentage(fee.getPercentage())
                .calculationRule(fee.getCalculationRule())
                .createdAt(fee.getCreatedAt())
                .build();
    }
}