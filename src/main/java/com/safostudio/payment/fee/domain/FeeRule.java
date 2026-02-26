package com.safostudio.payment.fee.domain;

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

    private String transactionType; // TRANSFER, TOP_UP, WITHDRAWAL, REFUND

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

    /**
     * Calculate fee based on transaction amount
     */
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
                    // percentage / 100 * transactionAmount
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
                // TODO: implement tiered calculation
                calculatedFee = BigDecimal.ZERO;
                break;

            default:
                throw new IllegalArgumentException("Unsupported fee type: " + feeType);
        }

        // Apply min/max constraints
        if (minAmount != null && calculatedFee.compareTo(minAmount) < 0) {
            calculatedFee = minAmount;
        }
        if (maxAmount != null && calculatedFee.compareTo(maxAmount) > 0) {
            calculatedFee = maxAmount;
        }

        return calculatedFee.setScale(2, RoundingMode.HALF_UP);
    }
}