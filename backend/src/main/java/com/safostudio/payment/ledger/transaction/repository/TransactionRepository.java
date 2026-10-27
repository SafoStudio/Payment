package com.safostudio.payment.ledger.transaction.repository;

import com.safostudio.payment.ledger.transaction.domain.Transaction;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, UUID>,
        PagingAndSortingRepository<Transaction, UUID> {

  @Query("SELECT * FROM transactions WHERE debit_wallet_id = :walletId OR credit_wallet_id = :walletId " +
          "ORDER BY created_at DESC LIMIT :limit")
  List<Transaction> findRecentTransactions(@Param("walletId") UUID walletId,
                                           @Param("limit") int limit);

  @Query("""
      SELECT * FROM transactions
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
  List<Transaction> findPage(
          @Param("status") String status,
          @Param("type") String type,
          @Param("currency") String currency,
          @Param("cursorTime") Instant cursorTime,
          @Param("cursorId") UUID cursorId,
          @Param("limit") int limit
  );
}