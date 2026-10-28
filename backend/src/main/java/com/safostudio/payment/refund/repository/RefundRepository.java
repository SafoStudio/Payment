package com.safostudio.payment.refund.repository;

import com.safostudio.payment.refund.domain.Refund;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRepository extends CrudRepository<Refund, UUID> {

  List<Refund> findByPaymentId(UUID paymentId);

  @Query("""
      SELECT * FROM refunds
      WHERE (:status::text IS NULL OR status = :status::text)
        AND (:currency IS NULL OR currency = :currency)
        AND (
          :cursorTime::timestamptz IS NULL
          OR (created_at, id) < (:cursorTime::timestamptz, :cursorId::uuid)
        )
      ORDER BY created_at DESC, id DESC
      LIMIT :limit
      """)
  List<Refund> findPage(
          @Param("status") String status,
          @Param("currency") String currency,
          @Param("cursorTime") Instant cursorTime,
          @Param("cursorId") UUID cursorId,
          @Param("limit") int limit
  );
}