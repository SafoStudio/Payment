package com.safostudio.payment.fee.api;

import com.safostudio.payment.fee.api.input.CreateFeeRuleInput;
import com.safostudio.payment.fee.api.input.UpdateFeeRuleInput;
import com.safostudio.payment.fee.domain.FeeRule;
import com.safostudio.payment.fee.service.FeeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FeeRuleController {

  private final FeeRuleService feeRuleService;

  @PreAuthorize("hasAuthority('fee:read')")
  @QueryMapping
  public List<FeeRule> feeRules() {
    return feeRuleService.findAll();
  }

  @PreAuthorize("hasAuthority('fee:read')")
  @QueryMapping
  public FeeRule feeRule(@Argument UUID id) {
    return feeRuleService.findById(id);
  }

  @PreAuthorize("hasAuthority('fee:read')")
  @QueryMapping
  public List<FeeRule> feeRulesByType(@Argument String transactionType) {
    return feeRuleService.findByTransactionType(transactionType);
  }

  @PreAuthorize("hasAuthority('fee_rule:manage')")
  @MutationMapping
  public FeeRule createFeeRule(@Argument CreateFeeRuleInput input) {
    return feeRuleService.create(input);
  }

  @PreAuthorize("hasAuthority('fee_rule:manage')")
  @MutationMapping
  public FeeRule updateFeeRule(@Argument UpdateFeeRuleInput input) {
    return feeRuleService.update(input);
  }

  @PreAuthorize("hasAuthority('fee_rule:manage')")
  @MutationMapping
  public Boolean deleteFeeRule(@Argument UUID id) {
    return feeRuleService.delete(id);
  }
}