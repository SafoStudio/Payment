package com.safostudio.payment.wallet.domain;

import com.safostudio.payment.wallet.exception.TransactionException;
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

    public enum TransactionType {
        TRANSFER,
        TOP_UP,
        WITHDRAWAL,
        REFUND
    }

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Creates a transfer transaction
     */
    public static Transaction createTransfer(UUID fromWallet, UUID toWallet,
                                             BigDecimal amount, String currency,
                                             String description) {
        validateAmount(amount);
        validateCurrencies(currency, currency); // self-check

        if (fromWallet.equals(toWallet)) {
            throw TransactionException.invalidWallet("transfer", fromWallet);
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

    /**
     * Creates a top-up transaction
     */
    public static Transaction createTopUp(UUID walletId, BigDecimal amount,
                                          String currency, String description) {
        validateAmount(amount);

        return Transaction.builder()
                .debitWalletId(UUID.fromString("00000000-0000-0000-0000-000000000001")) // SYSTEM_REVENUE
                .creditWalletId(walletId)
                .amount(amount)
                .currency(currency)
                .type(TransactionType.TOP_UP)
                .status(TransactionStatus.PENDING)
                .description(description)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Creates a withdrawal transaction
     */
    public static Transaction createWithdrawal(UUID walletId, BigDecimal amount,
                                               String currency, String description) {
        validateAmount(amount);

        return Transaction.builder()
                .debitWalletId(walletId)
                .creditWalletId(UUID.fromString("00000000-0000-0000-0000-000000000002")) // SYSTEM_FEES
                .amount(amount)
                .currency(currency)
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.PENDING)
                .description(description)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Completes the transaction successfully
     */
    public void complete() {
        if (status != TransactionStatus.PENDING) {
            throw TransactionException.notPending(status);
        }
        if (isCompleted()) {
            throw TransactionException.alreadyCompleted();
        }
        this.status = TransactionStatus.COMPLETED;
    }

    /**
     * Marks the transaction as failed
     */
    public void fail() {
        if (status != TransactionStatus.PENDING) {
            throw TransactionException.notPending(status);
        }
        this.status = TransactionStatus.FAILED;
    }

    /**
     * Cancels the transaction
     */
    public void cancel() {
        if (status != TransactionStatus.PENDING) {
            throw TransactionException.notPending(status);
        }
        this.status = TransactionStatus.CANCELLED;
    }

    /**
     * Validates transaction type
     */
    public void validateType(TransactionType expected) {
        if (this.type != expected) {
            throw TransactionException.invalidType(expected, this.type);
        }
    }

    /**
     * Validates transaction status
     */
    public void validateStatus(TransactionStatus expected) {
        if (this.status != expected) {
            throw TransactionException.invalidStatus(expected, this.status);
        }
    }

    /**
     * Validates currency match
     */
    public void validateCurrency(String expectedCurrency) {
        if (!this.currency.equals(expectedCurrency)) {
            throw TransactionException.currencyMismatch(expectedCurrency, this.currency);
        }
    }

    /**
     * Checks if transaction status
     */
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

    /**
     * Validates that amount is positive
     */
    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw TransactionException.amountMustBePositive();
        }
    }

    /**
     * Validates currencies match
     */
    private static void validateCurrencies(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw TransactionException.currencyMismatch(expected, actual);
        }
    }
}