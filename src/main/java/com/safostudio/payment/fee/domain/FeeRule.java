package com.safostudio.payment.fee.domain;

import com.safostudio.payment.shared.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("fee_rules")
public class FeeRule {

  @Id
  private UUID id;

  private String name;

  private String transactionType;

  private FeeType feeType;

  private BigDecimal fixedAmount;

  private BigDecimal percentage;

  private BigDecimal minAmount;

  private BigDecimal maxAmount;

  private String currency;

  private UUID fromWalletId;

  private UUID toWalletId;

  private boolean isActive;

  private Instant createdAt;

  private Instant updatedAt;

  public enum FeeType {
    FIXED,
    PERCENTAGE,
    MIXED,
    TIERED
  }

  public BigDecimal calculate(BigDecimal transactionAmount) {
    if (transactionAmount == null || transactionAmount.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal calculatedFee;

    switch (feeType) {
      case FIXED:
        calculatedFee = fixedAmount != null ? fixedAmount : BigDecimal.ZERO;
        break;

      case PERCENTAGE:
        if (percentage == null) {
          calculatedFee = BigDecimal.ZERO;
        } else {
          calculatedFee = transactionAmount
                  .multiply(percentage)
                  .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        break;

      case MIXED:
        BigDecimal percentPart = BigDecimal.ZERO;
        if (percentage != null) {
          percentPart = transactionAmount
                  .multiply(percentage)
                  .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        BigDecimal fixedPart = fixedAmount != null ? fixedAmount : BigDecimal.ZERO;
        calculatedFee = fixedPart.add(percentPart);
        break;

      case TIERED:
        throw BusinessException.validationError("TIERED fee type is not implemented");

      default:
        throw BusinessException.validationError("Unsupported fee type: " + feeType);
    }

    if (minAmount != null && calculatedFee.compareTo(minAmount) < 0) {
      calculatedFee = minAmount;
    }
    if (maxAmount != null && calculatedFee.compareTo(maxAmount) > 0) {
      calculatedFee = maxAmount;
    }

    return calculatedFee.setScale(2, RoundingMode.HALF_UP);
  }
}