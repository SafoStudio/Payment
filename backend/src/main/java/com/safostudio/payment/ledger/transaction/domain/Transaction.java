package com.safostudio.payment.ledger.transaction.domain;

import com.safostudio.payment.shared.exception.BusinessException;
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
@Table("transactions")
public class Transaction {

  @Id
  private UUID id;

  private UUID debitWalletId;

  private UUID creditWalletId;

  private BigDecimal amount;

  private String currency;

  private TransactionType type;

  private TransactionStatus status;

  private String description;

  private UUID referenceId;

  private Instant createdAt;

  public static Transaction createTransfer(UUID fromWallet, UUID toWallet,
                                           BigDecimal amount, String currency,
                                           String description) {
    validateAmount(amount);

    if (fromWallet.equals(toWallet)) {
      throw BusinessException.invalidWallet("transfer", fromWallet);
    }

    return Transaction.builder()
            .debitWalletId(fromWallet)
            .creditWalletId(toWallet)
            .amount(amount)
            .currency(currency)
            .type(TransactionType.TRANSFER)
            .status(TransactionStatus.PENDING)
            .description(description)
            .createdAt(Instant.now())
            .build();
  }

  public static Transaction createAdjustment(UUID debitWalletId, UUID creditWalletId,
                                             BigDecimal amount, String currency,
                                             String description) {
    validateAmount(amount);

    if (debitWalletId.equals(creditWalletId)) {
      throw BusinessException.sameWallet();
    }

    return Transaction.builder()
            .debitWalletId(debitWalletId)
            .creditWalletId(creditWalletId)
            .amount(amount)
            .currency(currency)
            .type(TransactionType.ADJUSTMENT)
            .status(TransactionStatus.PENDING)
            .description(description)
            .createdAt(Instant.now())
            .build();
  }

  public void complete() {
    if (status != TransactionStatus.PENDING) {
      throw BusinessException.transactionNotPending(status);
    }
    if (isCompleted()) {
      throw BusinessException.transactionAlreadyCompleted();
    }
    this.status = TransactionStatus.COMPLETED;
  }

  public void fail() {
    if (status != TransactionStatus.PENDING) {
      throw BusinessException.transactionNotPending(status);
    }
    this.status = TransactionStatus.FAILED;
  }

  public boolean isPending() {
    return status == TransactionStatus.PENDING;
  }
  public boolean isCompleted() {
    return status == TransactionStatus.COMPLETED;
  }
  public boolean isFailed() {
    return status == TransactionStatus.FAILED;
  }
  public boolean isCancelled() {
    return status == TransactionStatus.CANCELLED;
  }

  private static void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw BusinessException.invalidAmount();
    }
  }
}