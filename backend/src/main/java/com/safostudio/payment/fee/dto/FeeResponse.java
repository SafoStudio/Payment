package com.safostudio.payment.fee.dto;

import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.domain.FeeType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class FeeResponse {
  UUID id;
  UUID transactionId;
  UUID fromWalletId;
  UUID toWalletId;
  String amount;
  String currency;
  FeeType type;
  String percentage;
  String fixedAmount;
  String calculationRule;
  Instant createdAt;

  public static FeeResponse from(Fee fee) {
    return FeeResponse.builder()
            .id(fee.getId())
            .transactionId(fee.getTransactionId())
            .fromWalletId(fee.getFromWalletId())
            .toWalletId(fee.getToWalletId())
            .amount(toPlain(fee.getAmount()))
            .currency(fee.getCurrency())
            .type(fee.getType())
            .percentage(toPlain(fee.getPercentage()))
            .fixedAmount(toPlain(fee.getFixedAmount()))
            .calculationRule(fee.getCalculationRule())
            .createdAt(fee.getCreatedAt())
            .build();
  }

  private static String toPlain(java.math.BigDecimal value) {
    return value != null ? value.toPlainString() : null;
  }
}