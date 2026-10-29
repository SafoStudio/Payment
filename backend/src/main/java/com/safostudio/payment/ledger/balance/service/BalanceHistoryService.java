package com.safostudio.payment.ledger.balance.service;

import com.safostudio.payment.ledger.balance.domain.BalanceHistory;
import com.safostudio.payment.ledger.balance.dto.BalanceHistoryPageResponse;
import com.safostudio.payment.ledger.balance.dto.BalanceHistoryResponse;
import com.safostudio.payment.ledger.balance.repository.BalanceHistoryRepository;
import com.safostudio.payment.shared.cursor.CursorCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BalanceHistoryService {

  private static final int MAX_LIMIT = 100;
  private static final int DEFAULT_LIMIT = 20;

  private final BalanceHistoryRepository balanceHistoryRepository;

  @Transactional(readOnly = true)
  public BalanceHistoryPageResponse getBalanceHistory(
          UUID walletId,
          Integer limit,
          String after
  ) {
    int effectiveLimit = normalizeLimit(limit);
    CursorCodec.Cursor cursor = after != null && !after.isBlank()
            ? CursorCodec.decode(after)
            : null;

    Instant cursorTime = cursor != null ? cursor.createdAt() : null;
    UUID cursorId = cursor != null ? cursor.id() : null;

    List<BalanceHistory> fetched = balanceHistoryRepository.findPage(
            walletId,
            cursorTime,
            cursorId,
            effectiveLimit + 1
    );

    boolean hasMore = fetched.size() > effectiveLimit;
    List<BalanceHistory> page = hasMore ? fetched.subList(0, effectiveLimit) : fetched;

    String nextCursor = null;
    if (hasMore && !page.isEmpty()) {
      BalanceHistory last = page.getLast();
      nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
    }

    return new BalanceHistoryPageResponse(
            page.stream().map(BalanceHistoryResponse::from).toList(),
            nextCursor,
            hasMore
    );
  }

  private int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.clamp(limit, 1, MAX_LIMIT);
  }
}
