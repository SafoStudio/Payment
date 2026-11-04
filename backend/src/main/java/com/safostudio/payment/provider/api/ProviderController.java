package com.safostudio.payment.provider.api;

import com.safostudio.payment.provider.api.input.AddProviderCapabilityInput;
import com.safostudio.payment.provider.api.input.CreateProviderInput;
import com.safostudio.payment.provider.api.input.RotateProviderCredentialInput;
import com.safostudio.payment.provider.api.input.SetProviderCredentialInput;
import com.safostudio.payment.provider.api.input.UpdateProviderCapabilityInput;
import com.safostudio.payment.provider.api.input.UpdateProviderInput;
import com.safostudio.payment.provider.domain.ProviderCredential;
import com.safostudio.payment.provider.domain.ProviderStatus;
import com.safostudio.payment.provider.domain.ProviderType;
import com.safostudio.payment.provider.dto.CreateProviderRequest;
import com.safostudio.payment.provider.dto.ProviderCapabilityResponse;
import com.safostudio.payment.provider.dto.ProviderCredentialKeyResponse;
import com.safostudio.payment.provider.dto.ProviderPageResponse;
import com.safostudio.payment.provider.dto.ProviderResponse;
import com.safostudio.payment.provider.dto.UpdateProviderRequest;
import com.safostudio.payment.provider.service.ProviderCapabilityService;
import com.safostudio.payment.provider.service.ProviderCredentialService;
import com.safostudio.payment.provider.service.ProviderQueryService;
import com.safostudio.payment.provider.service.ProviderService;
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
public class ProviderController {

  private final ProviderQueryService queryService;
  private final ProviderService providerService;
  private final ProviderCapabilityService capabilityService;
  private final ProviderCredentialService credentialService;

  @PreAuthorize("hasAnyAuthority('provider:read:own','provider:manage:all')")
  @QueryMapping
  public ProviderPageResponse providers(@Argument ProviderStatus status,
                                        @Argument ProviderType type,
                                        @Argument Integer limit,
                                        @Argument String after) {
    return queryService.getProviders(
            status == null ? null : status.name(),
            type == null ? null : type.name(),
            limit, after);
  }

  @PreAuthorize("hasAnyAuthority('provider:read:own','provider:manage:all')")
  @QueryMapping
  public ProviderResponse provider(@Argument UUID id) {
    return queryService.getProvider(id);
  }

  @PreAuthorize("hasAnyAuthority('provider:read:own','provider:manage:all')")
  @QueryMapping
  public List<ProviderCapabilityResponse> providerCapabilities(@Argument UUID providerId) {
    return capabilityService.listByProvider(providerId).stream()
            .map(ProviderCapabilityResponse::fromDomain)
            .toList();
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @QueryMapping
  public List<ProviderCredentialKeyResponse> providerCredentialKeys(@Argument UUID providerId) {
    return credentialService.listCredentials(providerId);
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public ProviderResponse createProvider(@Argument CreateProviderInput input) {
    return providerService.create(new CreateProviderRequest(
            input.code(), input.name(), input.adapterKey(), input.type(), input.config()));
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public ProviderResponse updateProvider(@Argument UUID id, @Argument UpdateProviderInput input) {
    return providerService.update(id, new UpdateProviderRequest(
            input.name(), input.adapterKey(), input.type(), input.config()));
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public ProviderResponse activateProvider(@Argument UUID id) {
    return providerService.activate(id);
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public ProviderResponse degradeProvider(@Argument UUID id) {
    return providerService.degrade(id);
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public ProviderResponse disableProvider(@Argument UUID id) {
    return providerService.disable(id);
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public ProviderResponse markProviderChecked(@Argument UUID id) {
    return providerService.markChecked(id);
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public Boolean deleteProvider(@Argument UUID id) {
    providerService.delete(id);
    return true;
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public ProviderCapabilityResponse addProviderCapability(@Argument UUID providerId,
                                                          @Argument AddProviderCapabilityInput input) {
    return ProviderCapabilityResponse.fromDomain(capabilityService.add(
            providerId, input.method(), input.currency(),
            input.minAmount(), input.maxAmount(), input.enabled()));
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public ProviderCapabilityResponse updateProviderCapability(@Argument UUID capabilityId,
                                                             @Argument UpdateProviderCapabilityInput input) {
    return ProviderCapabilityResponse.fromDomain(capabilityService.update(
            capabilityId, input.method(), input.currency(),
            input.minAmount(), input.maxAmount(), input.enabled()));
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public Boolean removeProviderCapability(@Argument UUID capabilityId) {
    capabilityService.remove(capabilityId);
    return true;
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public ProviderCredentialKeyResponse setProviderCredential(@Argument UUID providerId,
                                                             @Argument SetProviderCredentialInput input) {
    ProviderCredential credential = credentialService.set(
            providerId, input.credentialKey(), input.value());
    return ProviderCredentialKeyResponse.fromDomain(credential);
  }

  @PreAuthorize("hasAuthority('provider:manage:all')")
  @MutationMapping
  public ProviderCredentialKeyResponse rotateProviderCredential(@Argument UUID providerId,
                                                                @Argument RotateProviderCredentialInput input) {
    ProviderCredential credential = credentialService.rotate(
            providerId, input.credentialKey(), input.value());
    return ProviderCredentialKeyResponse.fromDomain(credential);
  }
}