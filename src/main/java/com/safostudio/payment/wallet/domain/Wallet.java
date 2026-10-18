package com.safostudio.payment.wallet.domain;

import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.shared.util.MoneyUtils;
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

  public void debit(BigDecimal amount) {
    MoneyUtils.validateAmount(amount);
    validateForTransfer();

    if (balance.compareTo(amount) < 0) {
      throw BusinessException.insufficientFunds(balance, amount, currency);
    }
    this.balance = balance.subtract(amount);
  }

  public void credit(BigDecimal amount) {
    MoneyUtils.validateAmount(amount);

    if (status == WalletStatus.CLOSED) {
      throw BusinessException.walletClosed();
    }
    this.balance = balance.add(amount);
  }

  public boolean isActive() {
    return status == WalletStatus.ACTIVE;
  }

  public void validateForTransfer() {
    if (status != WalletStatus.ACTIVE) {
      throw BusinessException.walletNotActive(status);
    }
  }

  public void block() {
    if (status != WalletStatus.ACTIVE) {
      throw BusinessException.walletNotActive(status);
    }
    this.status = WalletStatus.BLOCKED;
  }

  public void unblock() {
    if (status != WalletStatus.BLOCKED) {
      throw BusinessException.walletNotActive(status);
    }
    this.status = WalletStatus.ACTIVE;
  }

  public void close() {
    if (balance.compareTo(BigDecimal.ZERO) != 0) {
      throw BusinessException.walletNonZeroBalance(balance, currency);
    }
    this.status = WalletStatus.CLOSED;
  }
}