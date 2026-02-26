package com.safostudio.payment.fee.controller;

import com.safostudio.payment.fee.controller.input.CreateFeeRuleInput;
import com.safostudio.payment.fee.controller.input.UpdateFeeRuleInput;
import com.safostudio.payment.fee.domain.FeeRule;
import com.safostudio.payment.fee.service.FeeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FeeRuleController {

    private final FeeRuleService feeRuleService;

    @QueryMapping
    public List<FeeRule> feeRules() {
        return feeRuleService.findAll();
    }

    @QueryMapping
    public FeeRule feeRule(@Argument UUID id) {
        return feeRuleService.findById(id);
    }

    @QueryMapping
    public List<FeeRule> feeRulesByType(@Argument String transactionType) {
        return feeRuleService.findByTransactionType(transactionType);
    }

    @MutationMapping
    public FeeRule createFeeRule(@Argument CreateFeeRuleInput input) {
        return feeRuleService.create(input);
    }

    @MutationMapping
    public FeeRule updateFeeRule(@Argument UpdateFeeRuleInput input) {
        return feeRuleService.update(input);
    }

    @MutationMapping
    public Boolean deleteFeeRule(@Argument UUID id) {
        return feeRuleService.delete(id);
    }
}