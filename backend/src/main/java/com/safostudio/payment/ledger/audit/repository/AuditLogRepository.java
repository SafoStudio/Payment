package com.safostudio.payment.ledger.audit.repository;

import com.safostudio.payment.ledger.audit.domain.AuditLog;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends CrudRepository<AuditLog, UUID> {

  @Query("""
            SELECT * FROM audit_log
            WHERE (:entityType::text IS NULL OR entity_type = :entityType::text)
              AND (:action::text IS NULL OR action = :action::text)
              AND (:userId::uuid IS NULL OR user_id = :userId::uuid)
              AND (
                :cursorTime::timestamptz IS NULL
                OR (created_at, id) < (:cursorTime::timestamptz, :cursorId::uuid)
              )
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """)
  List<AuditLog> findPage(
          @Param("entityType") String entityType,
          @Param("action") String action,
          @Param("userId") UUID userId,
          @Param("cursorTime") Instant cursorTime,
          @Param("cursorId") UUID cursorId,
          @Param("limit") int limit
  );
}