package com.safostudio.payment.fee.service;

import com.safostudio.payment.fee.api.input.CreateFeeRuleInput;
import com.safostudio.payment.fee.api.input.UpdateFeeRuleInput;
import com.safostudio.payment.fee.domain.FeeRule;
import com.safostudio.payment.fee.domain.FeeType;
import com.safostudio.payment.fee.repository.FeeRuleRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeRuleService {

  private final FeeRuleRepository feeRuleRepository;

  @Transactional(readOnly = true)
  public List<FeeRule> findAll() {
    return (List<FeeRule>) feeRuleRepository.findAll();
  }

  @Transactional(readOnly = true)
  public FeeRule findById(UUID id) {
    return feeRuleRepository.findById(id)
            .orElseThrow(() -> BusinessException.feeRuleNotFound(id));
  }

  @Transactional(readOnly = true)
  public List<FeeRule> findByTransactionType(String transactionType) {
    return feeRuleRepository.findByTransactionTypeAndIsActiveTrue(transactionType);
  }

  @Transactional
  public FeeRule create(CreateFeeRuleInput input) {
    FeeRule feeRule = FeeRule.builder()
            .id(null)
            .name(input.name())
            .transactionType(input.transactionType())
            .feeType(parseFeeType(input.feeType()))
            .fixedAmount(input.fixedAmount())
            .percentage(input.percentage())
            .minAmount(input.minAmount())
            .maxAmount(input.maxAmount())
            .currency(input.currency())
            .fromWalletId(input.fromWalletId())
            .toWalletId(input.toWalletId())
            .isActive(input.isActive() != null ? input.isActive() : true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    return feeRuleRepository.save(feeRule);
  }

  @Transactional
  public FeeRule update(UpdateFeeRuleInput input) {
    FeeRule existing = findById(input.id());

    FeeRule.FeeRuleBuilder builder = existing.toBuilder();

    if (input.name() != null) builder.name(input.name());
    if (input.transactionType() != null) builder.transactionType(input.transactionType());
    if (input.feeType() != null) builder.feeType(parseFeeType(input.feeType()));
    if (input.fixedAmount() != null) builder.fixedAmount(input.fixedAmount());
    if (input.percentage() != null) builder.percentage(input.percentage());
    if (input.minAmount() != null) builder.minAmount(input.minAmount());
    if (input.maxAmount() != null) builder.maxAmount(input.maxAmount());
    if (input.currency() != null) builder.currency(input.currency());
    if (input.fromWalletId() != null) builder.fromWalletId(input.fromWalletId());
    if (input.toWalletId() != null) builder.toWalletId(input.toWalletId());
    if (input.isActive() != null) builder.isActive(input.isActive());

    FeeRule updated = builder.build();
    return feeRuleRepository.save(updated);
  }

  @Transactional
  public Boolean delete(UUID id) {
    if (feeRuleRepository.existsById(id)) {
      feeRuleRepository.deleteById(id);
      return true;
    }
    return false;
  }

  private FeeType parseFeeType(String feeType) {
    try {
      return FeeType.valueOf(feeType);
    } catch (IllegalArgumentException e) {
      throw BusinessException.validationError("Invalid fee type: " + feeType);
    }
  }
}