package com.safostudio.payment.provider.service;

import com.safostudio.payment.provider.domain.Provider;
import com.safostudio.payment.provider.domain.ProviderCapability;
import com.safostudio.payment.provider.dto.ProviderPageResponse;
import com.safostudio.payment.provider.dto.ProviderResponse;
import com.safostudio.payment.provider.repository.ProviderCapabilityRepository;
import com.safostudio.payment.provider.repository.ProviderRepository;
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

@Service
@RequiredArgsConstructor
public class ProviderQueryService {

  private static final int MAX_LIMIT = 100;
  private static final int DEFAULT_LIMIT = 20;

  private final ProviderRepository providerRepository;
  private final ProviderCapabilityRepository capabilityRepository;
  private final ProviderCredentialService credentialService;

  @Transactional(readOnly = true)
  public ProviderPageResponse getProviders(String status, String type, Integer limit, String after) {
    int effectiveLimit = normalizeLimit(limit);
    CursorCodec.Cursor cursor = after != null && !after.isBlank()
            ? CursorCodec.decode(after)
            : null;

    Instant cursorTime = cursor != null ? cursor.createdAt() : null;
    UUID cursorId = cursor != null ? cursor.id() : null;

    List<Provider> fetched = providerRepository.findPage(
            status,
            type,
            cursorTime,
            cursorId,
            effectiveLimit + 1
    );

    boolean hasMore = fetched.size() > effectiveLimit;
    List<Provider> page = hasMore ? fetched.subList(0, effectiveLimit) : fetched;

    String nextCursor = null;
    if (hasMore && !page.isEmpty()) {
      Provider last = page.getLast();
      nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
    }

    Map<UUID, List<ProviderCapability>> capabilitiesByProvider = page.isEmpty()
            ? Map.of()
            : capabilityRepository.findByProviderIds(page.stream().map(Provider::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(ProviderCapability::getProviderId));

    return new ProviderPageResponse(
            page.stream()
                    .map(p -> ProviderResponse.fromDomain(
                            p, capabilitiesByProvider.getOrDefault(p.getId(), List.of()), false))
                    .toList(),
            nextCursor,
            hasMore
    );
  }

  @Transactional(readOnly = true)
  public ProviderResponse getProvider(UUID id) {
    Provider provider = providerRepository.findById(id)
            .orElseThrow(() -> BusinessException.providerNotFound(id));
    List<ProviderCapability> capabilities = capabilityRepository.findByProviderId(id);
    boolean hasCredentials = credentialService.hasCredentials(id);
    return ProviderResponse.fromDomain(provider, capabilities, hasCredentials);
  }

  private int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.clamp(limit, 1, MAX_LIMIT);
  }
}