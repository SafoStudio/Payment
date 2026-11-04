package com.safostudio.payment.fee.service;

import com.safostudio.payment.fee.api.input.CreateFeeRuleInput;
import com.safostudio.payment.fee.api.input.UpdateFeeRuleInput;
import com.safostudio.payment.fee.domain.FeeRule;
import com.safostudio.payment.fee.domain.FeeType;
import com.safostudio.payment.fee.repository.FeeRuleRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class FeeRuleService {

  private final FeeRuleRepository feeRuleRepository;

  @Transactional(readOnly = true)
  public List<FeeRule> findAll() {
    return StreamSupport.stream(feeRuleRepository.findAll().spliterator(), false)
            .toList();
  }

  @Transactional(readOnly = true)
  public FeeRule findById(UUID id) {
    return findByIdInternal(id);
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
            .isActive(input.isActive() == null || input.isActive())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    return feeRuleRepository.save(feeRule);
  }

  /**
   * Full replacement (PUT semantics): every mutable field is written as sent.
   * Nullable money/currency/wallet fields are cleared when null, so switching
   * fee type (e.g. FIXED -> PERCENTAGE) drops the now-irrelevant value.
   * createdAt is preserved; updatedAt is refreshed.
   */
  @Transactional
  public FeeRule update(UpdateFeeRuleInput input) {
    FeeRule existing = findByIdInternal(input.id());

    if (input.name() == null || input.name().isBlank()) {
      throw BusinessException.validationError("Fee rule name is required");
    }
    if (input.transactionType() == null || input.transactionType().isBlank()) {
      throw BusinessException.validationError("transactionType is required");
    }
    if (input.feeType() == null || input.feeType().isBlank()) {
      throw BusinessException.validationError("feeType is required");
    }

    FeeRule updated = existing.toBuilder()
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
            .isActive(input.isActive() != null && input.isActive())
            .updatedAt(Instant.now())
            .build();

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

  private FeeRule findByIdInternal(UUID id) {
    return feeRuleRepository.findById(id)
            .orElseThrow(() -> BusinessException.feeRuleNotFound(id));
  }

  private FeeType parseFeeType(String feeType) {
    try {
      return FeeType.valueOf(feeType);
    } catch (IllegalArgumentException e) {
      throw BusinessException.validationError("Invalid fee type: " + feeType);
    }
  }
}