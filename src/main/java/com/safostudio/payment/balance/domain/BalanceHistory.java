package com.safostudio.payment.balance.domain;

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
@Table("balance_history")
public class BalanceHistory {

    @Id
    private UUID id;

    private UUID walletId;

    private BigDecimal previousBalance;

    private BigDecimal newBalance;

    private BigDecimal delta;

    private UUID transactionId;

    private String operationType;

    private String currency;

    private Instant createdAt;

    /**
     * Creates a balance history entry
     */
    public static BalanceHistory create(UUID walletId, BigDecimal previousBalance,
                                        BigDecimal newBalance, UUID transactionId,
                                        String operationType, String currency) {
        return BalanceHistory.builder()
                .id(null)
                .walletId(walletId)
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .delta(newBalance.subtract(previousBalance))
                .transactionId(transactionId)
                .operationType(operationType)
                .currency(currency)
                .createdAt(Instant.now())
                .build();
    }
}