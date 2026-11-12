package com.safostudio.payment.notification.service;

import com.safostudio.payment.notification.domain.Notification;
import com.safostudio.payment.notification.dto.NotificationPageResponse;
import com.safostudio.payment.notification.dto.NotificationResponse;
import com.safostudio.payment.notification.repository.NotificationRepository;
import com.safostudio.payment.shared.cursor.CursorCodec;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private static final int MAX_LIMIT = 100;
  private static final int DEFAULT_LIMIT = 20;

  private final NotificationRepository notificationRepository;

  @Transactional(readOnly = true)
  public NotificationPageResponse findPage(String status, String type, Integer limit, String after) {
    int effectiveLimit = normalizeLimit(limit);
    CursorCodec.Cursor cursor = after != null && !after.isBlank()
            ? CursorCodec.decode(after)
            : null;

    Instant cursorTime = cursor != null ? cursor.createdAt() : null;
    UUID cursorId = cursor != null ? cursor.id() : null;

    List<Notification> fetched = notificationRepository.findPage(
            status, type, cursorTime, cursorId, effectiveLimit + 1);

    boolean hasMore = fetched.size() > effectiveLimit;
    List<Notification> page = hasMore ? fetched.subList(0, effectiveLimit) : fetched;

    String nextCursor = null;
    if (hasMore && !page.isEmpty()) {
      Notification last = page.getLast();
      nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
    }

    return new NotificationPageResponse(
            page.stream().map(NotificationResponse::fromDomain).toList(),
            nextCursor,
            hasMore
    );
  }

  @Transactional(readOnly = true)
  public NotificationResponse get(UUID id) {
    return notificationRepository.findById(id)
            .map(NotificationResponse::fromDomain)
            .orElseThrow(() -> BusinessException.notificationNotFound(id));
  }

  private int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.clamp(limit, 1, MAX_LIMIT);
  }
}