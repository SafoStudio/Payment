package com.safostudio.payment.wallet.service.dto;

import com.safostudio.payment.wallet.domain.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;
    private UUID debitWalletId;
    private UUID creditWalletId;
    private BigDecimal amount;
    private String currency;
    private String type;
    private String status;
    private String description;
    private UUID referenceId;
    private Instant createdAt;

    public static TransactionResponse fromDomain(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .debitWalletId(transaction.getDebitWalletId())
                .creditWalletId(transaction.getCreditWalletId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .description(transaction.getDescription())
                .referenceId(transaction.getReferenceId())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}