package com.safostudio.payment.ledger.transaction.service;

import com.safostudio.payment.ledger.transaction.domain.Transaction;
import com.safostudio.payment.ledger.transaction.dto.TransactionPageResponse;
import com.safostudio.payment.ledger.transaction.dto.TransactionResponse;
import com.safostudio.payment.ledger.transaction.repository.TransactionRepository;
import com.safostudio.payment.shared.cursor.CursorCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionQueryService {

  private static final int MAX_LIMIT = 100;
  private static final int DEFAULT_LIMIT = 20;

  private final TransactionRepository transactionRepository;

  @Transactional(readOnly = true)
  public TransactionPageResponse getTransactions(
          String status,
          String type,
          String currency,
          Integer limit,
          String after
  ) {
    int effectiveLimit = normalizeLimit(limit);
    CursorCodec.Cursor cursor = after != null && !after.isBlank()
            ? CursorCodec.decode(after)
            : null;

    Instant cursorTime = cursor != null ? cursor.createdAt() : null;
    UUID cursorId = cursor != null ? cursor.id() : null;

    List<Transaction> fetched = transactionRepository.findPage(
            status,
            type,
            currency,
            cursorTime,
            cursorId,
            effectiveLimit + 1
    );

    boolean hasMore = fetched.size() > effectiveLimit;
    List<Transaction> page = hasMore ? fetched.subList(0, effectiveLimit) : fetched;

    String nextCursor = null;
    if (hasMore && !page.isEmpty()) {
      Transaction last = page.getLast();
      nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
    }

    return new TransactionPageResponse(
            page.stream().map(TransactionResponse::fromDomain).toList(),
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