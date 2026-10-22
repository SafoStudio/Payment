package com.safostudio.payment.wallet.repository;

import com.safostudio.payment.wallet.domain.Wallet;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface WalletRepository extends CrudRepository<Wallet, UUID> {

  List<Wallet> findAllByUserId(UUID userId);

  @Query("SELECT * FROM wallets WHERE user_id IN (:userIds)")
  List<Wallet> findAllByUserIdIn(@Param("userIds") Set<UUID> userIds);

  boolean existsByUserId(UUID userId);

  boolean existsByUserIdAndCurrency(UUID userId, String currency);

  @Query("SELECT * FROM wallets WHERE user_id IS NOT NULL")
  List<Wallet> findAllUserWallets();
}