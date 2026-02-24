package com.safostudio.payment.payment.repository;

import com.safostudio.payment.payment.domain.Payment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByTransactionId(UUID transactionId);
    List<Payment> findByWalletId(UUID walletId);
}