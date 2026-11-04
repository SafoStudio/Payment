package com.safostudio.payment.provider.service;

import com.safostudio.payment.provider.domain.PaymentMethod;
import com.safostudio.payment.provider.domain.ProviderCapability;
import com.safostudio.payment.provider.repository.ProviderCapabilityRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderCapabilityService {

  private final ProviderCapabilityRepository capabilityRepository;

  @Transactional(readOnly = true)
  public List<ProviderCapability> listByProvider(UUID providerId) {
    return capabilityRepository.findByProviderId(providerId);
  }

  @Transactional
  public ProviderCapability add(UUID providerId, PaymentMethod method, String currency,
                                BigDecimal minAmount, BigDecimal maxAmount, Boolean enabled) {
    if (capabilityRepository
            .findByProviderIdAndMethodAndCurrency(providerId, method.name(), currency)
            .isPresent()) {
      throw BusinessException.providerCapabilityAlreadyExists(providerId, method.name(), currency);
    }
    ProviderCapability capability = ProviderCapability.create(
            providerId, method, currency, minAmount, maxAmount, enabled);
    return capabilityRepository.save(capability);
  }

  @Transactional
  public ProviderCapability update(UUID capabilityId, PaymentMethod method, String currency,
                                   BigDecimal minAmount, BigDecimal maxAmount, Boolean enabled) {
    ProviderCapability capability = capabilityRepository.findById(capabilityId)
            .orElseThrow(() ->  BusinessException.providerCapabilityNotFound(capabilityId));
    if (!capability.getMethod().equals(method) || !capability.getCurrency().equals(currency)) {
      boolean duplicate = capabilityRepository
              .findByProviderIdAndMethodAndCurrency(capability.getProviderId(), method.name(), currency)
              .isPresent();
      if (duplicate) {
        throw BusinessException.providerCapabilityAlreadyExists(
                capability.getProviderId(), method.name(), currency);
      }
    }
    capability.update(method, currency, minAmount, maxAmount, enabled);
    return capabilityRepository.save(capability);
  }

  @Transactional
  public void remove(UUID capabilityId) {
    if (capabilityRepository.findById(capabilityId).isEmpty()) {
      throw BusinessException.providerCapabilityNotFound(capabilityId);
    }
    capabilityRepository.deleteById(capabilityId);
  }

  @Transactional
  public void deleteAll(UUID providerId) {
    capabilityRepository.deleteByProviderId(providerId);
  }
}