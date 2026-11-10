package com.safostudio.payment.provider.repository;

import com.safostudio.payment.provider.domain.ProviderCapability;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderCapabilityRepository extends CrudRepository<ProviderCapability, UUID> {

  List<ProviderCapability> findByProviderId(UUID providerId);

  Optional<ProviderCapability> findByProviderIdAndMethodAndCurrency(
          UUID providerId, String method, String currency);

  @Query("SELECT * FROM provider_capabilities WHERE provider_id IN (:providerIds)")
  List<ProviderCapability> findByProviderIds(@Param("providerIds") Collection<UUID> providerIds);

  @Query("""
      SELECT * FROM provider_capabilities
      WHERE provider_id IN (:providerIds)
        AND method = :method::text
        AND currency = :currency::text
        AND enabled = TRUE
      """)
  List<ProviderCapability> findByProviderIdsAndMethodAndCurrency(
          @Param("providerIds") Collection<UUID> providerIds,
          @Param("method") String method,
          @Param("currency") String currency
  );

  @Modifying
  @Query("DELETE FROM provider_capabilities WHERE provider_id = :providerId")
  void deleteByProviderId(@Param("providerId") UUID providerId);
}