package com.safostudio.payment.wallet.domain;

import com.safostudio.payment.wallet.exception.WalletException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("wallets")
public class Wallet {

    @Id
    private UUID id;

    private UUID userId;

    private BigDecimal balance;

    private String currency;

    private WalletStatus status;

    @Version
    private Long version;

    private Instant createdAt;

    private Instant updatedAt;

    public enum WalletStatus {
        ACTIVE,
        BLOCKED,
        CLOSED
    }

    /**
     * Withdrawal of funds from the wallet (debit)
     */
    public void debit(BigDecimal amount) {
        validateForTransfer();

        if (balance.compareTo(amount) < 0) {
            throw WalletException.insufficient(balance, amount, currency);
        }
        this.balance = balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Crediting funds to your wallet (credit)
     */
    public void credit(BigDecimal amount) {
        if (status == WalletStatus.CLOSED) {
            throw WalletException.closed();
        }
        this.balance = balance.add(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Checking wallet activity
     */
    public boolean isActive() {
        return status == WalletStatus.ACTIVE;
    }

    /**
     * Validating a wallet for transfer
     */
    public void validateForTransfer() {
        if (status != WalletStatus.ACTIVE) {
            throw WalletException.inactive(status);
        }
    }

    /**
     * Wallet blocking
     */
    public void block() {
        if (status == WalletStatus.ACTIVE) {
            this.status = WalletStatus.BLOCKED;
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Unlocking wallet
     */
    public void unblock() {
        if (status == WalletStatus.BLOCKED) {
            this.status = WalletStatus.ACTIVE;
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Closing a wallet (only if the balance is 0)
     */
    public void close() {
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw WalletException.nonZeroBalance(balance, currency);
        }
        this.status = WalletStatus.CLOSED;
        this.updatedAt = Instant.now();
    }
}