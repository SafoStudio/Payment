package com.safostudio.payment.ledger.outbox.repository;

import com.safostudio.payment.ledger.outbox.domain.OutboxEvent;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends CrudRepository<OutboxEvent, UUID> {

  Optional<OutboxEvent> findByEventId(UUID eventId);

  @Query("""
      SELECT * FROM outbox_events
      WHERE status IN ('PENDING', 'FAILED')
        AND next_attempt_at <= now()
      ORDER BY next_attempt_at ASC, created_at ASC
      LIMIT :limit
      FOR UPDATE SKIP LOCKED
      """)
  List<OutboxEvent> findDispatchable(@Param("limit") int limit);
}