package com.safostudio.payment.refund.service;

import com.safostudio.payment.ledger.outbox.service.OutboxService;
import com.safostudio.payment.refund.repository.RefundRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundFailureHandler {

  private final RefundRepository refundRepository;
  private final OutboxService outboxService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(UUID refundId, UUID providerId, String failureReason, UUID targetWalletId) {
    refundRepository.findById(refundId).ifPresent(refund -> {
      try {
        refund.fail(providerId, failureReason);
        refundRepository.save(refund);

        Map<String, Object> payload = new HashMap<>();
        payload.put("refundId", refund.getId().toString());
        payload.put("paymentId", refund.getPaymentId().toString());
        payload.put("amount", refund.getAmount().toPlainString());
        payload.put("currency", refund.getCurrency());
        payload.put("targetWalletId", targetWalletId != null ? targetWalletId.toString() : null);
        payload.put("providerId", providerId != null ? providerId.toString() : null);
        payload.put("providerReference", null);
        payload.put("failureReason", failureReason);
        payload.put("refundTransactionId", null);

        outboxService.append("Refund", refund.getId(), "RefundFailed", payload);

        log.debug("Refund {} marked as FAILED (reason={})", refundId, failureReason);
      } catch (BusinessException e) {
        log.warn("Could not mark refund {} as failed: {}", refundId, e.getMessage());
      }
    });
  }
}