package com.safostudio.payment.saga.service;

import com.safostudio.payment.ledger.outbox.service.OutboxService;
import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.refund.domain.Refund;
import com.safostudio.payment.refund.repository.RefundRepository;
import com.safostudio.payment.saga.domain.SagaInstance;
import com.safostudio.payment.saga.repository.SagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundSagaTimeoutHandler implements SagaTimeoutHandler {

  public static final String TIMEOUT_REASON = "TIMEOUT";

  private final SagaRepository sagaRepository;
  private final RefundRepository refundRepository;
  private final PaymentRepository paymentRepository;
  private final OutboxService outboxService;

  @Override
  public String supportedSagaType() {
    return RefundSagaService.SAGA_TYPE_REFUND_SYNC;
  }

  @Override
  public void timeOut(SagaInstance saga) {
    saga.timeOut("Saga exceeded timeout_at");
    sagaRepository.save(saga);

    refundRepository.findById(saga.getAggregateId()).ifPresent(refund -> {
      if (!refund.isCompleted() && !refund.isFailed()) {
        UUID targetWalletId = paymentRepository.findById(refund.getPaymentId())
                .map(Payment::getTargetWalletId)
                .orElse(null);
        refund.fail(refund.getProviderId(), TIMEOUT_REASON);
        Refund saved = refundRepository.save(refund);
        outboxService.append("Refund", saved.getId(), "RefundFailed",
                failurePayload(saved, targetWalletId, TIMEOUT_REASON));
        log.info("Saga {} timed out, refund {} marked FAILED (TIMEOUT)",
                saga.getId(), saved.getId());
      } else {
        log.info("Saga {} timed out, refund {} already terminal ({}), skipped",
                saga.getId(), refund.getId(), refund.getStatus());
      }
    });
  }

  private Map<String, Object> failurePayload(Refund refund, UUID targetWalletId, String failureReason) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("refundId", refund.getId().toString());
    payload.put("paymentId", refund.getPaymentId().toString());
    payload.put("amount", refund.getAmount().toPlainString());
    payload.put("currency", refund.getCurrency());
    payload.put("targetWalletId", targetWalletId != null ? targetWalletId.toString() : null);
    payload.put("providerId", refund.getProviderId() != null
            ? refund.getProviderId().toString() : null);
    payload.put("providerReference", null);
    payload.put("failureReason", failureReason);
    payload.put("refundTransactionId", null);
    return payload;
  }
}