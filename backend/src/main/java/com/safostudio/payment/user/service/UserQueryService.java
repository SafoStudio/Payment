package com.safostudio.payment.user.service;

import com.safostudio.payment.shared.cursor.CursorCodec;
import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.user.dto.UserPageResponse;
import com.safostudio.payment.user.dto.UserResponse;
import com.safostudio.payment.user.repository.RoleRepository;
import com.safostudio.payment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserQueryService {

  private static final int MAX_LIMIT = 100;
  private static final int DEFAULT_LIMIT = 20;

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  @Transactional(readOnly = true)
  public UserPageResponse getUsers(
          String status,
          String roleCode,
          Integer limit,
          String after
  ) {
    int effectiveLimit = normalizeLimit(limit);
    CursorCodec.Cursor cursor = after != null && !after.isBlank()
            ? CursorCodec.decode(after)
            : null;

    Instant cursorTime = cursor != null ? cursor.createdAt() : null;
    UUID cursorId = cursor != null ? cursor.id() : null;

    List<User> fetched = userRepository.findPage(
            status,
            roleCode,
            cursorTime,
            cursorId,
            effectiveLimit + 1
    );

    boolean hasMore = fetched.size() > effectiveLimit;
    List<User> page = hasMore ? fetched.subList(0, effectiveLimit) : fetched;

    String nextCursor = null;
    if (hasMore && !page.isEmpty()) {
      User last = page.getLast();
      nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
    }

    List<UserResponse> items = page.stream()
            .map(u -> UserResponse.fromDomain(
                    u,
                    roleRepository.findRoleCodesByUserId(u.getId()),
                    List.of(),
                    null
            ))
            .toList();

    return new UserPageResponse(items, nextCursor, hasMore);
  }

  private int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.clamp(limit, 1, MAX_LIMIT);
  }
}
