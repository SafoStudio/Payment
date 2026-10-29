package com.safostudio.payment.ledger.balance.api;

import com.safostudio.payment.ledger.balance.dto.BalanceHistoryPageResponse;
import com.safostudio.payment.ledger.balance.service.BalanceHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class BalanceHistoryController {

  private final BalanceHistoryService balanceHistoryService;

  @PreAuthorize("hasAnyAuthority('wallet:read:own', 'wallet:read:all')")
  @QueryMapping
  public BalanceHistoryPageResponse balanceHistory(
          @Argument UUID walletId,
          @Argument Integer limit,
          @Argument String after
  ) {
    return balanceHistoryService.getBalanceHistory(walletId, limit, after);
  }
}
