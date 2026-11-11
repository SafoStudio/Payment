package com.safostudio.payment.payment.service;

import com.safostudio.payment.ledger.transaction.domain.TransactionReference;
import com.safostudio.payment.ledger.transaction.dto.TransferRequest;
import com.safostudio.payment.ledger.transaction.service.TransferService;
import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.provider.adapter.ProviderAdapter;
import com.safostudio.payment.provider.adapter.ProviderAdapterRegistry;
import com.safostudio.payment.provider.adapter.ProviderRequest;
import com.safostudio.payment.provider.adapter.ProviderResult;
import com.safostudio.payment.provider.domain.Provider;
import com.safostudio.payment.provider.repository.ProviderRepository;
import com.safostudio.payment.provider.service.ProviderCredentialService;
import com.safostudio.payment.routing.dto.ResolvedProvider;
import com.safostudio.payment.routing.service.ProviderRoutingService;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessor {

  private static final String NO_PROVIDER = "NO_PROVIDER_AVAILABLE";
  private static final String LEDGER_FAILED = "LEDGER_FAILED";
  private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

  private final TransferService transferService;
  private final PaymentRepository paymentRepository;
  private final ProviderRoutingService providerRoutingService;
  private final ProviderAdapterRegistry adapterRegistry;
  private final ProviderCredentialService credentialService;
  private final ProviderRepository providerRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Payment processNew(Payment payment) {
    payment.markAsProcessing();
    paymentRepository.save(payment);
    return executeWithProvider(payment);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Payment processExisting(UUID paymentId) {
    Payment locked = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> BusinessException.paymentNotFound(paymentId));
    locked.markAsProcessing();
    paymentRepository.save(locked);
    return executeWithProvider(locked);
  }

  private Payment executeWithProvider(Payment payment) {
    ResolvedProvider resolved;
    try {
      resolved = providerRoutingService.resolve(payment.getMethod(), payment.getCurrency(), payment.getAmount());
    } catch (BusinessException e) {
      log.warn("No provider for payment {}: {}", payment.getId(), e.getMessage());
      payment.fail(null, NO_PROVIDER);
      return paymentRepository.save(payment);
    }

    Provider provider = providerRepository.findById(resolved.providerId()).orElse(null);
    if (provider == null || !provider.isActive()) {
      payment.fail(resolved.providerId(), NO_PROVIDER);
      return paymentRepository.save(payment);
    }

    ProviderAdapter adapter = adapterRegistry.get(provider.getAdapterKey());
    ProviderRequest request = new ProviderRequest(
            payment.getId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getMethod(),
            provider.getConfig(),
            Map.of(),
            credentialService.getDecrypted(provider.getId())
    );

    ProviderResult result;
    try {
      result = adapter.authorize(request);
    } catch (Exception e) {
      log.error("Adapter {} failed for payment {}", provider.getAdapterKey(), payment.getId(), e);
      payment.fail(provider.getId(), INTERNAL_ERROR);
      return paymentRepository.save(payment);
    }

    if (!result.isApproved()) {
      String reason = result.declineCode() != null ? result.declineCode() : "PROVIDER_ERROR";
      log.info("Payment {} not approved by provider {}: status={}, reason={}",
              payment.getId(), provider.getCode(), result.status(), reason);
      payment.fail(provider.getId(), reason);
      return paymentRepository.save(payment);
    }

    try {
      return executeTransfer(payment, provider, result.providerReference());
    } catch (BusinessException e) {
      log.error("Ledger failed after provider approval for payment {}: {}",
              payment.getId(), e.getMessage());
      payment.fail(provider.getId(), LEDGER_FAILED);
      paymentRepository.save(payment);
      throw e;
    }
  }

  private Payment executeTransfer(Payment payment, Provider provider, String providerReference) {
    TransferRequest request = TransferRequest.builder()
            .fromWalletId(payment.getWalletId())
            .toWalletId(payment.getTargetWalletId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .idempotencyKey(payment.getIdempotencyKey())
            .description(payment.getDescription())
            .build();

    var transaction = transferService.transfer(
            request,
            TransactionReference.payment(payment.getId())
    );
    payment.succeed(transaction.getId(), provider.getId(), providerReference);
    return paymentRepository.save(payment);
  }
}