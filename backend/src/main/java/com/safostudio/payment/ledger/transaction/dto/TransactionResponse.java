package com.safostudio.payment.ledger.transaction.dto;

import com.safostudio.payment.ledger.transaction.domain.Transaction;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class TransactionResponse {

  UUID id;
  UUID debitWalletId;
  UUID creditWalletId;
  String amount;
  String currency;
  String type;
  String status;
  String description;
  UUID referenceId;
  Instant createdAt;

  public static TransactionResponse fromDomain(Transaction transaction) {
    return TransactionResponse.builder()
            .id(transaction.getId())
            .debitWalletId(transaction.getDebitWalletId())
            .creditWalletId(transaction.getCreditWalletId())
            .amount(transaction.getAmount().toPlainString())
            .currency(transaction.getCurrency())
            .type(transaction.getType().name())
            .status(transaction.getStatus().name())
            .description(transaction.getDescription())
            .referenceId(transaction.getReferenceId())
            .createdAt(transaction.getCreatedAt())
            .build();
  }
}