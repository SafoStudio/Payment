package com.safostudio.payment.ledger.transaction.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class AdjustBalanceRequest {
  UUID walletId;
  BigDecimal amount;
  String reason;
  String idempotencyKey;
  String ipAddress;
  String userAgent;
}