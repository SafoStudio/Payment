package com.safostudio.payment.ledger.idempotency.repository;

import com.safostudio.payment.ledger.idempotency.domain.IdempotencyKey;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRepository extends CrudRepository<IdempotencyKey, UUID> {

  Optional<IdempotencyKey> findByOperationAndIdempotencyKey(String operation, String idempotencyKey);

  @Modifying
  @Query("DELETE FROM idempotency_keys WHERE expires_at < :now")
  void deleteExpired(@Param("now") Instant now);

  @Modifying
  @Query("""
            INSERT INTO idempotency_keys (id, operation, idempotency_key, status, request_fingerprint, response, expires_at, created_at)
            VALUES (gen_random_uuid(), :operation, :idempotencyKey, 'PROCESSING', :fingerprint, '{}'::jsonb, :expiresAt, NOW())
            ON CONFLICT (operation, idempotency_key) DO NOTHING
            """)
  int tryInsert(
          @Param("operation") String operation,
          @Param("idempotencyKey") String idempotencyKey,
          @Param("fingerprint") String fingerprint,
          @Param("expiresAt") Instant expiresAt
  );

  @Modifying
  @Query("""
            UPDATE idempotency_keys
            SET status = 'SUCCEEDED', response = :response
            WHERE operation = :operation AND idempotency_key = :idempotencyKey AND status = 'PROCESSING'
            """)
  int complete(
          @Param("operation") String operation,
          @Param("idempotencyKey") String idempotencyKey,
          @Param("response") String response
  );
}