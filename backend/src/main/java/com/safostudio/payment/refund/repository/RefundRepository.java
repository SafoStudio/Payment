package com.safostudio.payment.refund.repository;

import com.safostudio.payment.refund.domain.Refund;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends CrudRepository<Refund, UUID> {
    List<Refund> findByPaymentId(UUID paymentId);
    List<Refund> findByOriginalTransactionId(UUID transactionId);
    Optional<Refund> findByRefundTransactionId(UUID transactionId);
}