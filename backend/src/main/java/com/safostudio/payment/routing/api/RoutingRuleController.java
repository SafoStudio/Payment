package com.safostudio.payment.routing.api;

import com.safostudio.payment.provider.domain.PaymentMethod;
import com.safostudio.payment.routing.api.input.CreateRoutingRuleInput;
import com.safostudio.payment.routing.api.input.UpdateRoutingRuleInput;
import com.safostudio.payment.routing.dto.CreateRoutingRuleRequest;
import com.safostudio.payment.routing.dto.ResolvedProvider;
import com.safostudio.payment.routing.dto.RoutingRulePageResponse;
import com.safostudio.payment.routing.dto.RoutingRuleResponse;
import com.safostudio.payment.routing.dto.UpdateRoutingRuleRequest;
import com.safostudio.payment.routing.service.ProviderRoutingService;
import com.safostudio.payment.routing.service.RoutingRuleService;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class RoutingRuleController {

  private final RoutingRuleService routingRuleService;
  private final ProviderRoutingService providerRoutingService;

  @PreAuthorize("hasAnyAuthority('routing:read:all','routing:manage:all')")
  @QueryMapping
  public RoutingRulePageResponse routingRules(@Argument PaymentMethod method,
                                              @Argument String currency,
                                              @Argument Boolean enabled,
                                              @Argument Integer limit,
                                              @Argument String after) {
    return routingRuleService.getRoutingRules(
            method == null ? null : method.name(),
            currency, enabled, limit, after);
  }

  @PreAuthorize("hasAnyAuthority('routing:read:all','routing:manage:all')")
  @QueryMapping
  public RoutingRuleResponse routingRule(@Argument UUID id) {
    return routingRuleService.getRoutingRule(id);
  }

  @PreAuthorize("hasAuthority('routing:manage:all')")
  @QueryMapping
  public ResolvedProvider resolveProvider(@Argument PaymentMethod method,
                                          @Argument String currency,
                                          @Argument String amount) {
    BigDecimal parsed;
    try {
      parsed = new BigDecimal(amount);
    } catch (NumberFormatException e) {
      throw BusinessException.validationError("Invalid amount format: " + amount);
    }
    return providerRoutingService.resolve(method.name(), currency, parsed);
  }

  @PreAuthorize("hasAuthority('routing:manage:all')")
  @MutationMapping
  public RoutingRuleResponse createRoutingRule(@Argument CreateRoutingRuleInput input) {
    return routingRuleService.create(new CreateRoutingRuleRequest(
            input.method(), input.currency(), input.providerId(),
            input.priority(), input.weight()));
  }

  @PreAuthorize("hasAuthority('routing:manage:all')")
  @MutationMapping
  public RoutingRuleResponse updateRoutingRule(@Argument UUID id,
                                               @Argument UpdateRoutingRuleInput input) {
    return routingRuleService.update(id, new UpdateRoutingRuleRequest(
            input.priority(), input.weight(), input.enabled()));
  }

  @PreAuthorize("hasAuthority('routing:manage:all')")
  @MutationMapping
  public RoutingRuleResponse enableRoutingRule(@Argument UUID id) {
    return routingRuleService.enable(id);
  }

  @PreAuthorize("hasAuthority('routing:manage:all')")
  @MutationMapping
  public RoutingRuleResponse disableRoutingRule(@Argument UUID id) {
    return routingRuleService.disable(id);
  }

  @PreAuthorize("hasAuthority('routing:manage:all')")
  @MutationMapping
  public Boolean deleteRoutingRule(@Argument UUID id) {
    routingRuleService.delete(id);
    return true;
  }
}