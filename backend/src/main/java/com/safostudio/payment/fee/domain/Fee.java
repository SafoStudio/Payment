package com.safostudio.payment.fee.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("fees")
public class Fee {

  @Id
  private UUID id;

  private UUID transactionId;

  private UUID fromWalletId;

  private UUID toWalletId;

  private BigDecimal amount;

  private String currency;

  private FeeType type;

  private BigDecimal percentage;

  private BigDecimal fixedAmount;

  private String calculationRule;

  private Instant createdAt;

  public static Fee createFixed(UUID transactionId, UUID fromWalletId,
                                UUID toWalletId, BigDecimal amount,
                                String currency) {
    return Fee.builder()
            .id(null)
            .transactionId(transactionId)
            .fromWalletId(fromWalletId)
            .toWalletId(toWalletId)
            .amount(amount)
            .currency(currency)
            .type(FeeType.FIXED)
            .fixedAmount(amount)
            .createdAt(Instant.now())
            .build();
  }

  public static Fee createPercentage(UUID transactionId, UUID fromWalletId,
                                     UUID toWalletId, BigDecimal percentage,
                                     BigDecimal calculatedAmount, String currency) {
    return Fee.builder()
            .id(null)
            .transactionId(transactionId)
            .fromWalletId(fromWalletId)
            .toWalletId(toWalletId)
            .amount(calculatedAmount)
            .currency(currency)
            .type(FeeType.PERCENTAGE)
            .percentage(percentage)
            .calculationRule("percentage_of_transaction")
            .createdAt(Instant.now())
            .build();
  }

  public static Fee createMixed(UUID transactionId, UUID fromWalletId,
                                UUID toWalletId, BigDecimal percentage,
                                BigDecimal fixedAmount, BigDecimal calculatedAmount,
                                String currency) {
    return Fee.builder()
            .id(null)
            .transactionId(transactionId)
            .fromWalletId(fromWalletId)
            .toWalletId(toWalletId)
            .amount(calculatedAmount)
            .currency(currency)
            .type(FeeType.MIXED)
            .percentage(percentage)
            .fixedAmount(fixedAmount)
            .calculationRule("fixed_plus_percentage")
            .createdAt(Instant.now())
            .build();
  }
}