package com.safostudio.payment.routing.repository;

import com.safostudio.payment.routing.domain.RoutingRule;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoutingRuleRepository extends CrudRepository<RoutingRule, UUID>,
        PagingAndSortingRepository<RoutingRule, UUID> {

  @Query("""
      SELECT * FROM routing_rules
      WHERE (:method::text IS NULL OR method = :method::text)
        AND (:currency::text IS NULL OR currency = :currency::text)
        AND (:enabled::boolean IS NULL OR enabled = :enabled::boolean)
        AND (
          :cursorTime::timestamptz IS NULL
          OR (created_at, id) < (:cursorTime::timestamptz, :cursorId::uuid)
        )
      ORDER BY created_at DESC, id DESC
      LIMIT :limit
      """)
  List<RoutingRule> findPage(
          @Param("method") String method,
          @Param("currency") String currency,
          @Param("enabled") Boolean enabled,
          @Param("cursorTime") Instant cursorTime,
          @Param("cursorId") UUID cursorId,
          @Param("limit") int limit
  );

  @Query("""
      SELECT * FROM routing_rules
      WHERE method = :method::text
        AND currency = :currency::text
        AND enabled = TRUE
      ORDER BY priority ASC, weight DESC, created_at ASC
      """)
  List<RoutingRule> findCandidates(
          @Param("method") String method,
          @Param("currency") String currency
  );

  boolean existsByMethodAndCurrencyAndProviderId(String method, String currency, UUID providerId);
}