package com.safostudio.payment.ledger.transaction.api;

import com.safostudio.payment.ledger.transaction.api.input.TransactionFilterInput;
import com.safostudio.payment.ledger.transaction.dto.TransactionPageResponse;
import com.safostudio.payment.ledger.transaction.dto.TransactionResponse;
import com.safostudio.payment.ledger.transaction.service.TransactionQueryService;
import com.safostudio.payment.ledger.transaction.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TransactionController {

  private final TransferService transferService;
  private final TransactionQueryService transactionQueryService;

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

  @PreAuthorize("hasAnyAuthority('transaction:read:all', 'transaction:read:own')")
  @QueryMapping
  public TransactionPageResponse transactions(
          @Argument TransactionFilterInput filter,
          @Argument Integer limit,
          @Argument String after
  ) {
    String status = filter != null && filter.status() != null
            ? filter.status().name()
            : null;
    String type = filter != null && filter.type() != null
            ? filter.type().name()
            : null;
    String currency = filter != null ? filter.currency() : null;

    return transactionQueryService.getTransactions(status, type, currency, limit, after);
  }
}