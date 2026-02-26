package com.safostudio.payment.fee.repository;

import com.safostudio.payment.fee.domain.Fee;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeeRepository extends CrudRepository<Fee, UUID> {
    List<Fee> findByTransactionId(UUID transactionId);
    List<Fee> findByFromWalletId(UUID walletId);
    List<Fee> findByToWalletId(UUID walletId);
}