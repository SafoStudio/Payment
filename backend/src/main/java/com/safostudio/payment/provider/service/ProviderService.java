package com.safostudio.payment.provider.service;

import com.safostudio.payment.provider.adapter.ProviderAdapterRegistry;
import com.safostudio.payment.provider.domain.Provider;
import com.safostudio.payment.provider.dto.CreateProviderRequest;
import com.safostudio.payment.provider.dto.ProviderResponse;
import com.safostudio.payment.provider.dto.UpdateProviderRequest;
import com.safostudio.payment.provider.repository.ProviderRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderService {

  private final ProviderRepository providerRepository;
  private final ProviderCapabilityService capabilityService;
  private final ProviderCredentialService credentialService;
  private final ProviderAdapterRegistry adapterRegistry;

  @Transactional
  public ProviderResponse create(CreateProviderRequest request) {
    if (!adapterRegistry.contains(request.adapterKey())) {
      throw BusinessException.validationError("Unknown provider adapter: " + request.adapterKey());
    }
    if (providerRepository.findByCode(request.code()).isPresent()) {
      throw BusinessException.providerAlreadyExists("code", request.code());
    }
    Provider provider = Provider.create(
            request.code(),
            request.name(),
            request.adapterKey(),
            request.type(),
            request.config()
    );
    Provider saved = providerRepository.save(provider);
    return ProviderResponse.fromDomain(saved, List.of(), false);
  }

  @Transactional
  public ProviderResponse update(UUID id, UpdateProviderRequest request) {
    Provider provider = providerRepository.findById(id)
            .orElseThrow(() -> BusinessException.providerNotFound(id));
    if (!adapterRegistry.contains(request.adapterKey())) {
      throw BusinessException.validationError("Unknown provider adapter: " + request.adapterKey());
    }
    provider.update(request.name(), request.adapterKey(), request.type(), request.config());
    Provider saved = providerRepository.save(provider);
    boolean hasCredentials = credentialService.hasCredentials(id);
    return ProviderResponse.fromDomain(saved, capabilityService.listByProvider(id), hasCredentials);
  }

  @Transactional
  public ProviderResponse activate(UUID id) {
    return changeStatus(id, Provider::activate);
  }

  @Transactional
  public ProviderResponse degrade(UUID id) {
    return changeStatus(id, Provider::degrade);
  }

  @Transactional
  public ProviderResponse disable(UUID id) {
    return changeStatus(id, Provider::disable);
  }

  @Transactional
  public ProviderResponse markChecked(UUID id) {
    Provider provider = providerRepository.findById(id)
            .orElseThrow(() -> BusinessException.providerNotFound(id));
    provider.markChecked(java.time.Instant.now());
    Provider saved = providerRepository.save(provider);
    boolean hasCredentials = credentialService.hasCredentials(id);
    return ProviderResponse.fromDomain(saved, capabilityService.listByProvider(id), hasCredentials);
  }

  @Transactional
  public void delete(UUID id) {
    if (providerRepository.findById(id).isEmpty()) {
      throw BusinessException.providerNotFound(id);
    }
    capabilityService.deleteAll(id);
    credentialService.deleteAll(id);
    providerRepository.deleteById(id);
  }

  private ProviderResponse changeStatus(UUID id, java.util.function.Consumer<Provider> action) {
    Provider provider = providerRepository.findById(id)
            .orElseThrow(() -> BusinessException.providerNotFound(id));
    action.accept(provider);
    Provider saved = providerRepository.save(provider);
    boolean hasCredentials = credentialService.hasCredentials(id);
    return ProviderResponse.fromDomain(saved, capabilityService.listByProvider(id), hasCredentials);
  }
}