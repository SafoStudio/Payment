package com.safostudio.payment.fee.service;

import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.domain.FeeRule;
import com.safostudio.payment.fee.repository.FeeRuleRepository;
import com.safostudio.payment.fee.service.dto.CalculateFeeRequest;
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
    private static final UUID SYSTEM_FEES_WALLET =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    /**
     * Calculate fees for a transaction
     */
    public List<Fee> calculateFees(CalculateFeeRequest request) {
        List<Fee> fees = new ArrayList<>();
        List<FeeRule> rules = findApplicableRules(request);

        for (FeeRule rule : rules) {
            BigDecimal feeAmount = rule.calculate(request.getAmount());

            UUID toWalletId = rule.getToWalletId() != null ?
                    rule.getToWalletId() : SYSTEM_FEES_WALLET;

            Fee fee;
            if (rule.getFeeType() == FeeRule.FeeType.FIXED) {
                fee = Fee.createFixed(
                        null,
                        rule.getFromWalletId() != null ? rule.getFromWalletId() : request.getFromWalletId(),
                        toWalletId,
                        feeAmount,
                        request.getCurrency()
                );
            } else {
                fee = Fee.createPercentage(
                        null,
                        rule.getFromWalletId() != null ? rule.getFromWalletId() : request.getFromWalletId(),
                        toWalletId,
                        rule.getPercentage(),
                        feeAmount,
                        request.getCurrency()
                );
            }

            fees.add(fee);
        }

        return fees;
    }

    /**
     * Find all applicable fee rules
     */
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