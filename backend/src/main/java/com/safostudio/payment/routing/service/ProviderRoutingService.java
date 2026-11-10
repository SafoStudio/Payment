package com.safostudio.payment.routing.service;

import com.safostudio.payment.provider.domain.Provider;
import com.safostudio.payment.provider.domain.ProviderCapability;
import com.safostudio.payment.provider.domain.ProviderStatus;
import com.safostudio.payment.provider.repository.ProviderCapabilityRepository;
import com.safostudio.payment.provider.repository.ProviderRepository;
import com.safostudio.payment.routing.domain.RoutingRule;
import com.safostudio.payment.routing.dto.ResolvedProvider;
import com.safostudio.payment.routing.repository.RoutingRuleRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProviderRoutingService {

  private final RoutingRuleRepository routingRuleRepository;
  private final ProviderRepository providerRepository;
  private final ProviderCapabilityRepository capabilityRepository;

  @Transactional(readOnly = true)
  public ResolvedProvider resolve(String method, String currency, BigDecimal amount) {
    validateInput(method, currency, amount);

    List<RoutingRule> candidates = routingRuleRepository.findCandidates(method, currency);
    if (candidates.isEmpty()) {
      throw BusinessException.noProviderAvailable(method, currency, amount);
    }

    List<UUID> providerIds = candidates.stream()
            .map(RoutingRule::getProviderId)
            .distinct()
            .toList();

    Map<UUID, Provider> providersById =
            StreamSupport.stream(providerRepository.findAllById(providerIds).spliterator(), false)
                    .collect(Collectors.toMap(Provider::getId, p -> p));

    Map<UUID, ProviderCapability> capabilityByProviderId =
            capabilityRepository.findByProviderIdsAndMethodAndCurrency(providerIds, method, currency)
                    .stream()
                    .collect(Collectors.toMap(ProviderCapability::getProviderId, c -> c));

    for (RoutingRule rule : candidates) {
      Provider provider = providersById.get(rule.getProviderId());
      if (provider == null || provider.getStatus() != ProviderStatus.ACTIVE) {
        continue;
      }
      ProviderCapability capability = capabilityByProviderId.get(provider.getId());
      if (capability == null || !capability.supports(amount)) {
        continue;
      }
      return ResolvedProvider.from(rule, provider);
    }

    throw BusinessException.noProviderAvailable(method, currency, amount);
  }

  private void validateInput(String method, String currency, BigDecimal amount) {
    if (method == null || method.isBlank()) {
      throw BusinessException.validationError("Payment method is required");
    }
    if (currency == null || !currency.matches("^[A-Z]{3}$")) {
      throw BusinessException.invalidCurrency(currency);
    }
    if (amount == null || amount.signum() <= 0) {
      throw BusinessException.invalidAmount();
    }
  }
}