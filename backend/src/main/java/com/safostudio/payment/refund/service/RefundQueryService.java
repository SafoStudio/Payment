package com.safostudio.payment.refund.service;

import com.safostudio.payment.refund.domain.Refund;
import com.safostudio.payment.refund.dto.RefundPageResponse;
import com.safostudio.payment.refund.dto.RefundResponse;
import com.safostudio.payment.refund.repository.RefundRepository;
import com.safostudio.payment.shared.cursor.CursorCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundQueryService {

  private static final int MAX_LIMIT = 100;
  private static final int DEFAULT_LIMIT = 20;

  private final RefundRepository refundRepository;

  @Transactional(readOnly = true)
  public RefundPageResponse getRefunds(
          String status,
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

    List<Refund> fetched = refundRepository.findPage(
            status,
            currency,
            cursorTime,
            cursorId,
            effectiveLimit + 1
    );

    boolean hasMore = fetched.size() > effectiveLimit;
    List<Refund> page = hasMore ? fetched.subList(0, effectiveLimit) : fetched;

    String nextCursor = null;
    if (hasMore && !page.isEmpty()) {
      Refund last = page.getLast();
      nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
    }

    return new RefundPageResponse(
            page.stream().map(RefundResponse::from).toList(),
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