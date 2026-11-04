package com.safostudio.payment.ledger.audit.service;

import com.safostudio.payment.ledger.audit.domain.AuditLog;
import com.safostudio.payment.ledger.audit.dto.AuditLogPageResponse;
import com.safostudio.payment.ledger.audit.dto.AuditLogResponse;
import com.safostudio.payment.ledger.audit.repository.AuditLogRepository;
import com.safostudio.payment.shared.cursor.CursorCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

  private static final int MAX_LIMIT = 100;
  private static final int DEFAULT_LIMIT = 20;

  private final AuditLogRepository auditLogRepository;

  @Transactional(readOnly = true)
  public AuditLogPageResponse findPage(
          String entityType,
          String action,
          UUID userId,
          Integer limit,
          String after
  ) {
    int effectiveLimit = normalizeLimit(limit);
    CursorCodec.Cursor cursor = after != null && !after.isBlank()
            ? CursorCodec.decode(after)
            : null;

    Instant cursorTime = cursor != null ? cursor.createdAt() : null;
    UUID cursorId = cursor != null ? cursor.id() : null;

    List<AuditLog> fetched = auditLogRepository.findPage(
            blankToNull(entityType),
            blankToNull(action),
            userId,
            cursorTime,
            cursorId,
            effectiveLimit + 1
    );

    boolean hasMore = fetched.size() > effectiveLimit;
    List<AuditLog> page = hasMore ? fetched.subList(0, effectiveLimit) : fetched;

    String nextCursor = null;
    if (hasMore && !page.isEmpty()) {
      AuditLog last = page.getLast();
      nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
    }

    List<AuditLogResponse> items = page.stream()
            .map(AuditLogResponse::fromDomain)
            .toList();

    return new AuditLogPageResponse(items, nextCursor, hasMore);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.clamp(limit, 1, MAX_LIMIT);
  }
}
