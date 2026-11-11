package com.safostudio.payment.provider.service;

import com.safostudio.payment.provider.crypto.CryptoService;
import com.safostudio.payment.provider.domain.ProviderCredential;
import com.safostudio.payment.provider.dto.ProviderCredentialKeyResponse;
import com.safostudio.payment.provider.repository.ProviderCredentialRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderCredentialService {

  private final ProviderCredentialRepository credentialRepository;
  private final CryptoService cryptoService;

  @Transactional
  public ProviderCredential set(UUID providerId, String credentialKey, String plaintext) {
    if (credentialRepository.findByProviderIdAndCredentialKey(providerId, credentialKey).isPresent()) {
      throw BusinessException.providerCredentialAlreadyExists(providerId, credentialKey);
    }
    String encrypted = cryptoService.encrypt(plaintext);
    ProviderCredential credential = ProviderCredential.create(providerId, credentialKey, encrypted);
    return credentialRepository.save(credential);
  }

  @Transactional
  public ProviderCredential rotate(UUID providerId, String credentialKey, String plaintext) {
    ProviderCredential credential = credentialRepository
            .findByProviderIdAndCredentialKey(providerId, credentialKey)
            .orElseThrow(() -> BusinessException.providerCredentialNotFound(providerId, credentialKey));
    credential.rotate(cryptoService.encrypt(plaintext));
    return credentialRepository.save(credential);
  }

  @Transactional(readOnly = true)
  public List<ProviderCredentialKeyResponse> listCredentials(UUID providerId) {
    return credentialRepository.findByProviderId(providerId).stream()
            .map(ProviderCredentialKeyResponse::fromDomain)
            .toList();
  }

  /**
   * Decrypted credential map for adapter invocation. Plaintext values are short-lived:
   * they live only in the ProviderRequest passed to the adapter and must never be
   * persisted, logged, or returned to clients.
   */
  @Transactional(readOnly = true)
  public Map<String, String> getDecrypted(UUID providerId) {
    List<ProviderCredential> credentials = credentialRepository.findByProviderId(providerId);
    if (credentials.isEmpty()) {
      return Map.of();
    }
    Map<String, String> decrypted = new LinkedHashMap<>(credentials.size());
    for (ProviderCredential credential : credentials) {
      decrypted.put(credential.getCredentialKey(),
              cryptoService.decrypt(credential.getEncryptedValue()));
    }
    return Map.copyOf(decrypted);
  }

  @Transactional(readOnly = true)
  public boolean hasCredentials(UUID providerId) {
    return !credentialRepository.findByProviderId(providerId).isEmpty();
  }

  @Transactional
  public void deleteAll(UUID providerId) {
    credentialRepository.deleteByProviderId(providerId);
  }
}