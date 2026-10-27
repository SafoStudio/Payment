package com.safostudio.payment.payment.repository;

import com.safostudio.payment.payment.domain.Payment;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, UUID> {

  Optional<Payment> findByIdempotencyKey(String idempotencyKey);

  List<Payment> findByWalletId(UUID walletId);

  @Query("SELECT * FROM payments WHERE id = :id FOR UPDATE")
  Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

  @Query("""
      SELECT * FROM payments
      WHERE (:status::text IS NULL OR status = :status::text)
        AND (:type::text IS NULL OR type = :type::text)
        AND (:currency IS NULL OR currency = :currency)
        AND (
          :cursorTime::timestamptz IS NULL
          OR (created_at, id) < (:cursorTime::timestamptz, :cursorId::uuid)
        )
      ORDER BY created_at DESC, id DESC
      LIMIT :limit
      """)
  List<Payment> findPage(
          @Param("status") String status,
          @Param("type") String type,
          @Param("currency") String currency,
          @Param("cursorTime") Instant cursorTime,
          @Param("cursorId") UUID cursorId,
          @Param("limit") int limit
  );
}