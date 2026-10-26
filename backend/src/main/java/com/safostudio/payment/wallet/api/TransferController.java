package com.safostudio.payment.wallet.api;

import com.safostudio.payment.wallet.api.input.TopUpInput;
import com.safostudio.payment.wallet.api.input.TransferInput;
import com.safostudio.payment.wallet.service.TransferService;
import com.safostudio.payment.wallet.dto.TopUpRequest;
import com.safostudio.payment.ledger.transaction.dto.TransactionResponse;
import com.safostudio.payment.wallet.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TransferController {

  private final TransferService transferService;

  @PreAuthorize("hasAuthority('wallet:create')")
  @MutationMapping
  public TransactionResponse transfer(@Argument TransferInput input) {
    TransferRequest request = TransferRequest.builder()
            .fromWalletId(input.fromWalletId())
            .toWalletId(input.toWalletId())
            .amount(input.amount())
            .currency(input.currency())
            .description(input.description())
            .idempotencyKey(input.idempotencyKey())
            .build();
    return transferService.transfer(request);
  }

  @PreAuthorize("hasAuthority('wallet:create')")
  @MutationMapping
  public TransactionResponse topUp(@Argument TopUpInput input) {
    TopUpRequest request = TopUpRequest.builder()
            .walletId(input.walletId())
            .amount(input.amount())
            .currency(input.currency())
            .description(input.description())
            .idempotencyKey(input.idempotencyKey())
            .build();
    return transferService.topUp(request);
  }

  @PreAuthorize("hasAnyAuthority('transaction:read:all', 'transaction:read:own')")
  @QueryMapping
  public TransactionResponse transaction(@Argument UUID id) {
    return transferService.getTransaction(id);
  }

  @PreAuthorize("hasAnyAuthority('transaction:read:all', 'transaction:read:own')")
  @QueryMapping
  public List<TransactionResponse> walletTransactions(@Argument UUID walletId, @Argument int limit) {
    return transferService.getWalletTransactions(walletId, limit);
  }
}