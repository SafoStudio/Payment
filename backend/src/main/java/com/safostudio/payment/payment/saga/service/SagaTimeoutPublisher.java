package com.safostudio.payment.payment.saga.service;

import com.safostudio.payment.ledger.outbox.service.OutboxService;
import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.payment.saga.domain.SagaInstance;
import com.safostudio.payment.payment.saga.repository.SagaRepository;
import com.safostudio.payment.shared.rls.RlsSystemContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Times out RUNNING sagas whose timeout_at has passed.
 * <p>
 * Mirrors OutboxPublisher: @Scheduled + @Transactional, RlsSystemContext for
 * RLS-protected writes, FOR UPDATE SKIP LOCKED via SagaRepository.findTimedOut
 * so multiple publisher instances do not pick the same row.
 * <p>
 * On timeout: saga -> TIMED_OUT, payment (if still PENDING/PROCESSING) -> FAILED
 * via PaymentFailureHandler semantics are NOT reused here — we write the
 * PaymentFailed outbox directly and mark payment FAILED. Payment.fail is
 * idempotent w.r.t. SUCCEEDED (throws), so we only act on non-terminal payments.
 * <p>
 * PaymentFailed with failureReason=TIMEOUT reuses the existing eventType;
 * audit/notification handlers already understand it. No new handler needed.
 * <p>
 * Batch TX (one TX per batch) matches OutboxPublisher; per-event TX is a TD
 * when publisher scaling is introduced.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTimeoutPublisher {

  private static final String TIMEOUT_REASON = "TIMEOUT";

  private final SagaRepository sagaRepository;
  private final PaymentRepository paymentRepository;
  private final OutboxService outboxService;

  @Value("${payment.saga.batch-size:50}")
  private int batchSize;

  @Scheduled(fixedDelayString = "${payment.saga.poll-interval:PT30S}")
  @Transactional
  public void publish() {
    try (RlsSystemContext ignored = RlsSystemContext.enter()) {
      List<SagaInstance> batch = sagaRepository.findTimedOut(batchSize);
      if (batch.isEmpty()) {
        return;
      }
      for (SagaInstance saga : batch) {
        timeOut(saga);
      }
    }
  }

  private void timeOut(SagaInstance saga) {
    try {
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
    } catch (Exception e) {
      // One bad saga must not abort the batch. Same policy as OutboxPublisher:
      // log and continue; the row will be re-picked on the next poll if still RUNNING.
      log.error("Failed to time out saga {}: {}", saga.getId(), e.getMessage(), e);
    }
  }

  private Map<String, Object> failurePayload(Payment payment, String failureReason) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("paymentId", payment.getId().toString());
    payload.put("amount", payment.getAmount().toPlainString());
    payload.put("currency", payment.getCurrency());
    payload.put("method", payment.getMethod());
    payload.put("targetWalletId", payment.getTargetWalletId().toString());
    payload.put("providerId", payment.getProviderId() != null ? payment.getProviderId().toString() : null);
    payload.put("providerReference", null);
    payload.put("failureReason", failureReason);
    payload.put("transactionId", null);
    return payload;
  }
}