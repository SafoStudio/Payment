package com.safostudio.payment.notification.repository;

import com.safostudio.payment.notification.domain.Notification;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends CrudRepository<Notification, UUID> {

  @Query("""
      SELECT * FROM notifications
      WHERE (:status::text IS NULL OR status = :status::text)
        AND (:type::text IS NULL OR type = :type::text)
        AND (
          :cursorTime::timestamptz IS NULL
          OR (created_at, id) < (:cursorTime::timestamptz, :cursorId::uuid)
        )
      ORDER BY created_at DESC, id DESC
      LIMIT :limit
      """)
  List<Notification> findPage(
          @Param("status") String status,
          @Param("type") String type,
          @Param("cursorTime") Instant cursorTime,
          @Param("cursorId") UUID cursorId,
          @Param("limit") int limit
  );
}