package com.safostudio.payment.fee.repository;

import com.safostudio.payment.fee.domain.FeeRule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeRuleRepository extends CrudRepository<FeeRule, UUID> {
    List<FeeRule> findByTransactionTypeAndIsActiveTrue(String transactionType);
    Optional<FeeRule> findByTransactionTypeAndFromWalletIdAndIsActiveTrue(String transactionType, UUID fromWalletId);
    Optional<FeeRule> findByTransactionTypeAndToWalletIdAndIsActiveTrue(String transactionType, UUID toWalletId);
}