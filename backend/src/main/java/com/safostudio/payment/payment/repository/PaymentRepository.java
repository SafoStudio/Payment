package com.safostudio.payment.payment.repository;

import com.safostudio.payment.payment.domain.Payment;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, UUID> {

  Optional<Payment> findByIdempotencyKey(String idempotencyKey);

  List<Payment> findByWalletId(UUID walletId);

  @Query("SELECT * FROM payments WHERE id = :id FOR UPDATE")
  Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
}