package com.safostudio.payment.wallet.repository;

import com.safostudio.payment.wallet.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, UUID>,
        PagingAndSortingRepository<Transaction, UUID> {

    List<Transaction> findAllByDebitWalletIdOrCreditWalletIdOrderByCreatedAtDesc(
            UUID debitWalletId, UUID creditWalletId);

    Page<Transaction> findAllByDebitWalletIdOrCreditWalletId(
            UUID debitWalletId, UUID creditWalletId, Pageable pageable);

    @Query("SELECT * FROM transactions WHERE debit_wallet_id = :walletId OR credit_wallet_id = :walletId " +
            "ORDER BY created_at DESC LIMIT :limit")
    List<Transaction> findRecentTransactions(@Param("walletId") UUID walletId,
                                             @Param("limit") int limit);

    List<Transaction> findAllByReferenceId(UUID referenceId);

    List<Transaction> findAllByStatusAndCreatedAtBefore(
            Transaction.TransactionStatus status, Instant createdAt);

    @Query("SELECT COUNT(*) > 0 FROM transactions t " +
            "WHERE (t.debit_wallet_id = :walletId OR t.credit_wallet_id = :walletId) " +
            "AND t.created_at >= :since")
    boolean hasTransactionsSince(@Param("walletId") UUID walletId,
                                 @Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE credit_wallet_id = :walletId AND status = 'COMPLETED'")
    BigDecimal getTotalCredited(@Param("walletId") UUID walletId);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE debit_wallet_id = :walletId AND status = 'COMPLETED'")
    BigDecimal getTotalDebited(@Param("walletId") UUID walletId);
}