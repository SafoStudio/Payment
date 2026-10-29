package com.safostudio.payment.ledger.balance.dto;

import com.safostudio.payment.ledger.balance.domain.BalanceHistory;

import java.time.Instant;
import java.util.UUID;

public record BalanceHistoryResponse(
        UUID id,
        UUID walletId,
        String previousBalance,
        String newBalance,
        String delta,
        UUID transactionId,
        String operationType,
        String currency,
        Instant createdAt
) {
  public static BalanceHistoryResponse from(BalanceHistory h) {
    return new BalanceHistoryResponse(
            h.getId(),
            h.getWalletId(),
            h.getPreviousBalance().toPlainString(),
            h.getNewBalance().toPlainString(),
            h.getDelta().toPlainString(),
            h.getTransactionId(),
            h.getOperationType(),
            h.getCurrency(),
            h.getCreatedAt()
    );
  }
}
