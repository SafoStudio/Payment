package com.safostudio.payment.payment.saga.repository;

import com.safostudio.payment.payment.saga.domain.SagaInstance;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaRepository extends CrudRepository<SagaInstance, UUID> {

  Optional<SagaInstance> findByAggregateTypeAndAggregateIdAndSagaType(
          String aggregateType,
          UUID aggregateId,
          String sagaType
  );

  /**
   * Dispatchable timeouts: RUNNING sagas whose timeout_at has passed.
   * FOR UPDATE SKIP LOCKED — same pattern as OutboxEventRepository.findDispatchable,
   * so multiple publisher instances do not pick the same row.
   */
  @Query("""
      SELECT * FROM saga_instances
      WHERE status = 'RUNNING'
        AND timeout_at IS NOT NULL
        AND timeout_at <= now()
      ORDER BY timeout_at ASC, created_at ASC
      LIMIT :limit
      FOR UPDATE SKIP LOCKED
      """)
  List<SagaInstance> findTimedOut(@Param("limit") int limit);
}