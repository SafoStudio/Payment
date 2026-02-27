package com.safostudio.payment.wallet.repository;

import com.safostudio.payment.wallet.domain.Wallet;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface WalletRepository extends CrudRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    List<Wallet> findAllByUserId(UUID userId);

    @Query("SELECT * FROM wallets WHERE user_id IN (:userIds)")
    List<Wallet> findAllByUserIdIn(@Param("userIds") Set<UUID> userIds);

    List<Wallet> findAllByStatus(Wallet.WalletStatus status);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndCurrency(UUID userId, String currency);

    @Query("SELECT * FROM wallets WHERE user_id = :userId AND status = 'ACTIVE'")
    List<Wallet> findActiveWalletsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE wallets SET balance = :balance, version = version + 1, updated_at = NOW() " +
            "WHERE id = :walletId AND version = :version")
    boolean optimisticLockUpdate(@Param("walletId") UUID walletId,
                                 @Param("balance") BigDecimal balance,
                                 @Param("version") Long version);

    @Query("SELECT COALESCE(SUM(balance), 0) FROM wallets WHERE user_id = :userId")
    BigDecimal getTotalBalanceByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(*) > 0 FROM wallets WHERE id = :walletId AND status = 'ACTIVE'")
    boolean isWalletActive(@Param("walletId") UUID walletId);
}