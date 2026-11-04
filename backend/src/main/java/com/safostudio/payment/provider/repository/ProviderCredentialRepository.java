package com.safostudio.payment.provider.repository;

import com.safostudio.payment.provider.domain.ProviderCredential;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderCredentialRepository extends CrudRepository<ProviderCredential, UUID> {

  List<ProviderCredential> findByProviderId(UUID providerId);

  Optional<ProviderCredential> findByProviderIdAndCredentialKey(UUID providerId, String credentialKey);

  @Modifying
  @Query("DELETE FROM provider_credentials WHERE provider_id = :providerId")
  void deleteByProviderId(@Param("providerId") UUID providerId);
}