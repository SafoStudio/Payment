package com.safostudio.payment.fee.service;

import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.domain.FeeRule;
import com.safostudio.payment.fee.repository.FeeRuleRepository;
import com.safostudio.payment.fee.dto.CalculateFeeRequest;
import com.safostudio.payment.shared.config.SystemWalletsProperties;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeeCalculator {

  private final FeeRuleRepository feeRuleRepository;
  private final SystemWalletsProperties systemWalletsProperties;

  public List<Fee> calculateFees(CalculateFeeRequest request) {
    List<Fee> fees = new ArrayList<>();
    List<FeeRule> rules = findApplicableRules(request);

    for (FeeRule rule : rules) {
      BigDecimal feeAmount = rule.calculate(request.getAmount());

      UUID toWalletId = rule.getToWalletId() != null ?
              rule.getToWalletId() : systemWalletsProperties.getFeeWalletId();

      UUID fromWalletId = rule.getFromWalletId() != null ?
              rule.getFromWalletId() : request.getFromWalletId();

      Fee fee = switch (rule.getFeeType()) {
        case FIXED -> Fee.createFixed(
                null, fromWalletId, toWalletId, feeAmount, request.getCurrency());
        case PERCENTAGE -> Fee.createPercentage(
                null, fromWalletId, toWalletId, rule.getPercentage(), feeAmount, request.getCurrency());
        case MIXED -> Fee.createMixed(
                null, fromWalletId, toWalletId,
                rule.getPercentage(), rule.getFixedAmount(), feeAmount, request.getCurrency());
        case TIERED -> throw BusinessException.validationError("TIERED fee type is not implemented");
      };

      fees.add(fee);
    }

    return fees;
  }

  private List<FeeRule> findApplicableRules(CalculateFeeRequest request) {
    List<FeeRule> rules = new ArrayList<>();

    var fromWalletRule = feeRuleRepository
            .findByTransactionTypeAndFromWalletIdAndIsActiveTrue(
                    request.getTransactionType(), request.getFromWalletId());
    fromWalletRule.ifPresent(rules::add);

    var toWalletRule = feeRuleRepository
            .findByTransactionTypeAndToWalletIdAndIsActiveTrue(
                    request.getTransactionType(), request.getToWalletId());
    toWalletRule.ifPresent(rules::add);

    if (rules.isEmpty()) {
      rules.addAll(feeRuleRepository
              .findByTransactionTypeAndIsActiveTrue(request.getTransactionType()));
    }

    return rules;
  }
}