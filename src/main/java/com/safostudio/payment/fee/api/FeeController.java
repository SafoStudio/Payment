package com.safostudio.payment.fee.api;

import com.safostudio.payment.fee.api.input.FeeInput;
import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.domain.FeeType;
import com.safostudio.payment.fee.service.FeeService;
import com.safostudio.payment.fee.dto.FeeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FeeController {

  private final FeeService feeService;

  @QueryMapping
  public List<FeeResponse> feesByTransaction(@Argument UUID transactionId) {
    return feeService.getFeesByTransaction(transactionId);
  }

  @QueryMapping
  public List<FeeResponse> feesByWallet(@Argument UUID walletId) {
    return feeService.getFeesByWallet(walletId);
  }

  @MutationMapping
  public FeeResponse createFee(@Argument FeeInput input) {
    Fee fee = Fee.builder()
            .id(null)
            .transactionId(input.transactionId())
            .fromWalletId(input.fromWalletId())
            .toWalletId(input.toWalletId())
            .amount(input.amount())
            .currency(input.currency())
            .type(FeeType.valueOf(input.type()))
            .percentage(input.percentage())
            .calculationRule(input.calculationRule())
            .createdAt(Instant.now())
            .build();

    return feeService.saveFee(fee);
  }
}