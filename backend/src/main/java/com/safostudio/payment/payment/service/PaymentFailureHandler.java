package com.safostudio.payment.payment.service;

import com.safostudio.payment.ledger.outbox.service.OutboxService;
import com.safostudio.payment.payment.repository.PaymentRepository;
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
public class PaymentFailureHandler {

  private final PaymentRepository paymentRepository;
  private final OutboxService outboxService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(UUID paymentId, UUID providerId, String failureReason) {
    paymentRepository.findById(paymentId).ifPresent(payment -> {
      try {
        payment.fail(providerId, failureReason);
        paymentRepository.save(payment);

        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", payment.getId().toString());
        payload.put("amount", payment.getAmount().toPlainString());
        payload.put("currency", payment.getCurrency());
        payload.put("method", payment.getMethod());
        payload.put("targetWalletId", payment.getTargetWalletId().toString());
        payload.put("providerId", providerId != null ? providerId.toString() : null);
        payload.put("providerReference", null);
        payload.put("failureReason", failureReason);
        payload.put("transactionId", null);

        outboxService.append("Payment", payment.getId(), "PaymentFailed", payload);

        log.debug("Payment {} marked as FAILED (reason={})", paymentId, failureReason);
      } catch (BusinessException e) {
        log.warn("Could not mark payment {} as failed: {}", paymentId, e.getMessage());
      }
    });
  }
}