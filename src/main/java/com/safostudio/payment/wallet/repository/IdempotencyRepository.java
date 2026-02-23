package com.safostudio.payment.wallet.repository;

import com.safostudio.payment.wallet.domain.IdempotencyKey;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IdempotencyRepository extends CrudRepository<IdempotencyKey, String> {

    Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("DELETE FROM idempotency_keys WHERE expires_at < :now")
    void deleteExpired(@Param("now") Instant now);

    boolean existsByIdempotencyKey(String idempotencyKey);
}