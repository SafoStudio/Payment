package com.safostudio.payment.refund.service;

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
import com.safostudio.payment.refund.domain.Refund;
import com.safostudio.payment.refund.repository.RefundRepository;
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
public class RefundProcessor {

  private static final String NO_PROVIDER = "NO_PROVIDER_AVAILABLE";
  private static final String LEDGER_FAILED = "LEDGER_FAILED";
  private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

  private final RefundRepository refundRepository;
  private final PaymentRepository paymentRepository;
  private final TransferService transferService;
  private final ProviderRepository providerRepository;
  private final ProviderAdapterRegistry adapterRegistry;
  private final ProviderCredentialService credentialService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Refund process(Refund refund) {
    UUID paymentId = refund.getPaymentId();

    Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> BusinessException.paymentNotFound(paymentId));

    if (refund.getAmount().compareTo(payment.getAmount()) > 0) {
      throw BusinessException.refundExceedsPayment(refund.getAmount(), payment.getAmount());
    }

    if (payment.getProviderId() == null) {
      refund.process();
      refund.fail(null, NO_PROVIDER);
      return refundRepository.save(refund);
    }

    Provider provider = providerRepository.findById(payment.getProviderId()).orElse(null);
    if (provider == null || !provider.isActive()) {
      refund.process();
      refund.fail(payment.getProviderId(), NO_PROVIDER);
      return refundRepository.save(refund);
    }

    refund.process();
    refund = refundRepository.save(refund);

    ProviderAdapter adapter = adapterRegistry.get(provider.getAdapterKey());
    ProviderRequest request = new ProviderRequest(
            payment.getId(),
            refund.getAmount(),
            refund.getCurrency(),
            payment.getMethod(),
            provider.getConfig(),
            Map.of(),
            credentialService.getDecrypted(provider.getId())
    );

    ProviderResult result;
    try {
      result = adapter.refund(request);
    } catch (Exception e) {
      log.error("Adapter {} failed for refund {}", provider.getAdapterKey(), refund.getId(), e);
      refund.fail(provider.getId(), INTERNAL_ERROR);
      return refundRepository.save(refund);
    }

    if (!result.isApproved()) {
      String reason = result.declineCode() != null ? result.declineCode() : "PROVIDER_ERROR";
      log.info("Refund {} not approved by provider {}: status={}, reason={}",
              refund.getId(), provider.getCode(), result.status(), reason);
      refund.fail(provider.getId(), reason);
      return refundRepository.save(refund);
    }

    try {
      TransferRequest transferRequest = TransferRequest.builder()
              .fromWalletId(payment.getTargetWalletId())
              .toWalletId(payment.getWalletId())
              .amount(refund.getAmount())
              .currency(refund.getCurrency())
              .idempotencyKey("refund-" + refund.getId())
              .description("Refund for payment: " + payment.getId())
              .build();

      var transaction = transferService.transfer(
              transferRequest,
              TransactionReference.refund(refund.getId())
      );

      refund.complete(transaction.getId(), provider.getId(), result.providerReference());

      payment.refund();
      paymentRepository.save(payment);

      return refundRepository.save(refund);

    } catch (BusinessException e) {
      log.error("Ledger failed after provider approval for refund {}: {}",
              refund.getId(), e.getMessage());
      refund.fail(provider.getId(), LEDGER_FAILED);
      refundRepository.save(refund);
      throw e;
    }
  }
}