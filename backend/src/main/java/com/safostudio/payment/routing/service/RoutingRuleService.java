package com.safostudio.payment.routing.service;

import com.safostudio.payment.provider.domain.Provider;
import com.safostudio.payment.provider.repository.ProviderRepository;
import com.safostudio.payment.routing.domain.RoutingRule;
import com.safostudio.payment.routing.dto.CreateRoutingRuleRequest;
import com.safostudio.payment.routing.dto.RoutingRulePageResponse;
import com.safostudio.payment.routing.dto.RoutingRuleResponse;
import com.safostudio.payment.routing.dto.UpdateRoutingRuleRequest;
import com.safostudio.payment.routing.repository.RoutingRuleRepository;
import com.safostudio.payment.shared.cursor.CursorCodec;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class RoutingRuleService {

  private static final int MAX_LIMIT = 100;
  private static final int DEFAULT_LIMIT = 20;

  private final RoutingRuleRepository routingRuleRepository;
  private final ProviderRepository providerRepository;

  @Transactional(readOnly = true)
  public RoutingRulePageResponse getRoutingRules(String method, String currency,
                                                 Boolean enabled, Integer limit, String after) {
    int effectiveLimit = normalizeLimit(limit);
    CursorCodec.Cursor cursor = after != null && !after.isBlank()
            ? CursorCodec.decode(after)
            : null;

    Instant cursorTime = cursor != null ? cursor.createdAt() : null;
    UUID cursorId = cursor != null ? cursor.id() : null;

    List<RoutingRule> fetched = routingRuleRepository.findPage(
            method, currency, enabled, cursorTime, cursorId, effectiveLimit + 1);

    boolean hasMore = fetched.size() > effectiveLimit;
    List<RoutingRule> page = hasMore ? fetched.subList(0, effectiveLimit) : fetched;

    String nextCursor = null;
    if (hasMore && !page.isEmpty()) {
      RoutingRule last = page.getLast();
      nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
    }

    Map<UUID, Provider> providersById = page.isEmpty()
            ? Map.of()
            : StreamSupport.stream(
                    providerRepository.findAllById(page.stream().map(RoutingRule::getProviderId).toList())
                            .spliterator(),
                    false)
            .collect(Collectors.toMap(Provider::getId, p -> p));

    return new RoutingRulePageResponse(
            page.stream()
                    .map(r -> RoutingRuleResponse.fromDomain(
                            r, providersById.get(r.getProviderId())))
                    .toList(),
            nextCursor,
            hasMore
    );
  }

  @Transactional(readOnly = true)
  public RoutingRuleResponse getRoutingRule(UUID id) {
    RoutingRule rule = routingRuleRepository.findById(id)
            .orElseThrow(() -> BusinessException.routingRuleNotFound(id));
    Provider provider = providerRepository.findById(rule.getProviderId()).orElse(null);
    return RoutingRuleResponse.fromDomain(rule, provider);
  }

  @Transactional
  public RoutingRuleResponse create(CreateRoutingRuleRequest request) {
    Provider provider = providerRepository.findById(request.providerId())
            .orElseThrow(() -> BusinessException.providerNotFound(request.providerId()));

    String method = request.method().name();

    if (routingRuleRepository.existsByMethodAndCurrencyAndProviderId(
            method, request.currency(), request.providerId())) {
      throw BusinessException.routingRuleAlreadyExists(
              method, request.currency(), request.providerId());
    }

    RoutingRule rule = RoutingRule.create(
            method, request.currency(), request.providerId(),
            request.priority(), request.weight());

    RoutingRule saved = routingRuleRepository.save(rule);
    return RoutingRuleResponse.fromDomain(saved, provider);
  }

  @Transactional
  public RoutingRuleResponse update(UUID id, UpdateRoutingRuleRequest request) {
    RoutingRule rule = routingRuleRepository.findById(id)
            .orElseThrow(() -> BusinessException.routingRuleNotFound(id));

    rule.update(request.priority(), request.weight(), request.enabled());

    RoutingRule saved = routingRuleRepository.save(rule);
    Provider provider = providerRepository.findById(saved.getProviderId()).orElse(null);
    return RoutingRuleResponse.fromDomain(saved, provider);
  }

  @Transactional
  public RoutingRuleResponse enable(UUID id) {
    return changeEnabled(id, RoutingRule::enable);
  }

  @Transactional
  public RoutingRuleResponse disable(UUID id) {
    return changeEnabled(id, RoutingRule::disable);
  }

  @Transactional
  public void delete(UUID id) {
    if (!routingRuleRepository.existsById(id)) {
      throw BusinessException.routingRuleNotFound(id);
    }
    routingRuleRepository.deleteById(id);
  }

  private RoutingRuleResponse changeEnabled(UUID id,
                                            java.util.function.Consumer<RoutingRule> action) {
    RoutingRule rule = routingRuleRepository.findById(id)
            .orElseThrow(() -> BusinessException.routingRuleNotFound(id));
    action.accept(rule);
    RoutingRule saved = routingRuleRepository.save(rule);
    Provider provider = providerRepository.findById(saved.getProviderId()).orElse(null);
    return RoutingRuleResponse.fromDomain(saved, provider);
  }

  private int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.clamp(limit, 1, MAX_LIMIT);
  }
}