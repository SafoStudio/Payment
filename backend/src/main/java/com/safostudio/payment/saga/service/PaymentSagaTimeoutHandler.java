package com.safostudio.payment.saga.service;

import com.safostudio.payment.ledger.outbox.service.OutboxService;
import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.saga.domain.SagaInstance;
import com.safostudio.payment.saga.repository.SagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSagaTimeoutHandler implements SagaTimeoutHandler {

  public static final String TIMEOUT_REASON = "TIMEOUT";

  private final SagaRepository sagaRepository;
  private final PaymentRepository paymentRepository;
  private final OutboxService outboxService;

  @Override
  public String supportedSagaType() {
    return PaymentSagaService.SAGA_TYPE_PAYMENT_SYNC;
  }

  @Override
  public void timeOut(SagaInstance saga) {
    saga.timeOut("Saga exceeded timeout_at");
    sagaRepository.save(saga);

    paymentRepository.findById(saga.getAggregateId()).ifPresent(payment -> {
      if (!payment.isSucceeded() && !payment.isFailed()) {
        payment.fail(payment.getProviderId(), TIMEOUT_REASON);
        Payment saved = paymentRepository.save(payment);
        outboxService.append("Payment", saved.getId(), "PaymentFailed",
                failurePayload(saved, TIMEOUT_REASON));
        log.info("Saga {} timed out, payment {} marked FAILED (TIMEOUT)",
                saga.getId(), saved.getId());
      } else {
        log.info("Saga {} timed out, payment {} already terminal ({}), skipped",
                saga.getId(), payment.getId(), payment.getStatus());
      }
    });
  }

  private Map<String, Object> failurePayload(Payment payment, String failureReason) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("paymentId", payment.getId().toString());
    payload.put("amount", payment.getAmount().toPlainString());
    payload.put("currency", payment.getCurrency());
    payload.put("method", payment.getMethod());
    payload.put("targetWalletId", payment.getTargetWalletId().toString());
    payload.put("providerId", payment.getProviderId() != null
            ? payment.getProviderId().toString() : null);
    payload.put("providerReference", null);
    payload.put("failureReason", failureReason);
    payload.put("transactionId", null);
    return payload;
  }
}