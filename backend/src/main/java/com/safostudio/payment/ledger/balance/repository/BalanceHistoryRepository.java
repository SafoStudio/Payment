package com.safostudio.payment.ledger.balance.repository;

import com.safostudio.payment.ledger.balance.domain.BalanceHistory;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BalanceHistoryRepository extends CrudRepository<BalanceHistory, UUID> {

  @Query("""
      SELECT * FROM balance_history
      WHERE wallet_id = :walletId
        AND (
          :cursorTime::timestamptz IS NULL
          OR (created_at, id) < (:cursorTime::timestamptz, :cursorId::uuid)
        )
      ORDER BY created_at DESC, id DESC
      LIMIT :limit
      """)
  List<BalanceHistory> findPage(
          @Param("walletId") UUID walletId,
          @Param("cursorTime") Instant cursorTime,
          @Param("cursorId") UUID cursorId,
          @Param("limit") int limit
  );
}
