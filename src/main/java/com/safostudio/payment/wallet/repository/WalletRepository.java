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
import java.util.UUID;

@Repository
public interface WalletRepository extends CrudRepository<Wallet, UUID> {

    Optional<Wallet> findByOwnerId(String ownerId);

    List<Wallet> findAllByOwnerId(String ownerId);

    List<Wallet> findAllByStatus(Wallet.WalletStatus status);

    boolean existsByOwnerId(String ownerId);

    boolean existsByOwnerIdAndCurrency(String ownerId, String currency);

    @Query("SELECT * FROM wallets WHERE owner_id = :ownerId AND status = 'ACTIVE'")
    List<Wallet> findActiveWalletsByOwner(@Param("ownerId") String ownerId);

    @Modifying
    @Query("UPDATE wallets SET balance = :balance, version = version + 1, updated_at = NOW() " +
            "WHERE id = :walletId AND version = :version")
    boolean optimisticLockUpdate(@Param("walletId") UUID walletId,
                                 @Param("balance") BigDecimal balance,
                                 @Param("version") Long version);

    @Query("SELECT SUM(balance) FROM wallets WHERE owner_id = :ownerId")
    BigDecimal getTotalBalanceByOwner(@Param("ownerId") String ownerId);

    @Query("SELECT COUNT(*) > 0 FROM wallets WHERE id = :walletId AND status = 'ACTIVE'")
    boolean isWalletActive(@Param("walletId") UUID walletId);
}