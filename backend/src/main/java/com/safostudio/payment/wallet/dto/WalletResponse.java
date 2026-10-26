package com.safostudio.payment.wallet.dto;

import com.safostudio.payment.wallet.domain.Wallet;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class WalletResponse {

  UUID id;
  UUID userId;
  String balance;
  String currency;
  String status;
  Instant createdAt;
  Instant updatedAt;

  public static WalletResponse fromDomain(Wallet wallet) {
    return WalletResponse.builder()
            .id(wallet.getId())
            .userId(wallet.getUserId())
            .balance(wallet.getBalance().toPlainString())
            .currency(wallet.getCurrency())
            .status(wallet.getStatus().name())
            .createdAt(wallet.getCreatedAt())
            .updatedAt(wallet.getUpdatedAt())
            .build();
  }
}