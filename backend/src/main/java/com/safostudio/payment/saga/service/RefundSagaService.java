package com.safostudio.payment.saga.service;

import com.safostudio.payment.refund.domain.Refund;
import com.safostudio.payment.refund.repository.RefundRepository;
import com.safostudio.payment.refund.service.RefundProcessor;
import com.safostudio.payment.saga.domain.SagaInstance;
import com.safostudio.payment.saga.domain.SagaStatus;
import com.safostudio.payment.saga.domain.SagaStep;
import com.safostudio.payment.saga.repository.SagaRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the REFUND_SYNC saga.
 * <p>
 * Two-phase by design, matching the refund flow: createRefund only persists
 * the refund (PENDING) and starts the saga (startRefundSaga, no execute);
 * processRefund runs the saga (runRefundSaga). This mirrors the existing
 * createRefund/processRefund split in RefundController and gives A a natural
 * place to insert AWAIT_WEBHOOK between start and execute.
 * <p>
 * RefundProcessor.process is the step executor — not rewritten. Step outcome
 * is derived from the refund state: isCompleted() proves ledger ran
 * (markLedgerReached), isFailed() is an AUTHORIZE-stage failure, a thrown
 * BusinessException is a ledger failure (compensation).
 * <p>
 * Resume scope on this slice: only PENDING refunds (first run from
 * processRefund, or a restart after TIMED_OUT while still PENDING).
 * PROCESSING is not resumable — Refund.process() requires PENDING and is not
 * idempotent; deferred to A where provider status / webhook makes the outcome
 * knowable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundSagaService {

  public static final String AGGREGATE_TYPE = "REFUND";
  public static final String SAGA_TYPE_REFUND_SYNC = "REFUND_SYNC";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final SagaRepository sagaRepository;
  private final RefundRepository refundRepository;
  private final RefundProcessor refundProcessor;

  @Value("${payment.saga.timeout:PT2M}")
  private Duration timeout;

  /**
   * Starts a saga for a freshly persisted refund. Does NOT execute steps —
   * execution happens in runRefundSaga (processRefund). Called from
   * RefundService.createRefund inside the aggregate transaction.
   */
  @Transactional
  public void startRefundSaga(Refund refund) {
    SagaInstance saga = SagaInstance.start(
            SAGA_TYPE_REFUND_SYNC,
            AGGREGATE_TYPE,
            refund.getId(),
            SagaStep.INITIATE,
            snapshot(refund, null, null),
            Instant.now().plus(timeout)
    );
    sagaRepository.save(saga);
  }

  /**
   * Runs the saga: first run from processRefund (RUNNING/INITIATE), or resume
   * after TIMED_OUT. Terminal -> reject. Only PENDING refunds are resumable.
   */
  @Transactional
  public Refund runRefundSaga(UUID refundId) {
    SagaInstance saga = sagaRepository
            .findByAggregateTypeAndAggregateIdAndSagaType(
                    AGGREGATE_TYPE, refundId, SAGA_TYPE_REFUND_SYNC)
            .orElseThrow(() -> BusinessException.internalError(
                    "Saga not found for refund: " + refundId));

    if (saga.getStatus() == SagaStatus.TIMED_OUT) {
      saga.restartAfterTimeout(SagaStep.AUTHORIZE, Instant.now().plus(timeout));
      saga = sagaRepository.save(saga);
    } else if (saga.getStatus() != SagaStatus.RUNNING) {
      throw BusinessException.internalError(
              "Cannot resume saga %s in terminal status %s"
                      .formatted(saga.getId(), saga.getStatus()));
    }

    Refund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> BusinessException.refundNotFound(refundId));

    if (refund.getStatus() != com.safostudio.payment.refund.domain.RefundStatus.PENDING) {
      throw BusinessException.internalError(
              "Cannot resume refund %s in status %s: processor path for non-PENDING is deferred to A"
                      .formatted(refund.getId(), refund.getStatus()));
    }

    if (saga.getCurrentStep() == SagaStep.INITIATE) {
      saga.advanceTo(SagaStep.AUTHORIZE, snapshot(refund, null, null));
      saga = sagaRepository.save(saga);
    }

    return execute(refund, saga);
  }

  private Refund execute(Refund refund, SagaInstance saga) {
    try {
      Refund processed = refundProcessor.process(refund);

      if (processed.isCompleted()) {
        String ledgerSnapshot = snapshot(processed, processed.getProviderId(),
                processed.getProviderReference());
        saga.markLedgerReached(ledgerSnapshot);
        sagaRepository.save(saga);
        saga.complete(ledgerSnapshot);
        sagaRepository.save(saga);
        log.debug("Saga {} completed for refund {}", saga.getId(), refund.getId());
      } else if (processed.isFailed()) {
        saga.fail(saga.getCurrentStep(), processed.getFailureReason(),
                snapshot(processed, processed.getProviderId(), null));
        sagaRepository.save(saga);
        log.debug("Saga {} failed for refund {} (reason={})",
                saga.getId(), refund.getId(), processed.getFailureReason());
      } else {
        saga.fail(saga.getCurrentStep(), "UNEXPECTED_REFUND_STATUS",
                snapshot(processed, processed.getProviderId(), null));
        sagaRepository.save(saga);
        log.warn("Saga {} unexpected refund status {} for refund {}",
                saga.getId(), processed.getStatus(), refund.getId());
      }
      return processed;
    } catch (BusinessException ledgerFailure) {
      saga.beginCompensation(ledgerFailure.getMessage());
      sagaRepository.save(saga);

      saga.completeCompensation(snapshot(refund, refund.getProviderId(), null));
      sagaRepository.save(saga);

      log.warn("Saga {} compensated for refund {} (reason={})",
              saga.getId(), refund.getId(), ledgerFailure.getMessage());
      throw ledgerFailure;
    }
  }

  private String snapshot(Refund refund, UUID providerId, String providerReference) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("refundId", refund.getId().toString());
    payload.put("paymentId", refund.getPaymentId().toString());
    payload.put("amount", refund.getAmount().toPlainString());
    payload.put("currency", refund.getCurrency());
    payload.put("status", refund.getStatus().name());
    payload.put("providerId", providerId != null ? providerId.toString() : null);
    payload.put("providerReference", providerReference);
    return serialize(payload);
  }

  private String serialize(Map<String, Object> payload) {
    try {
      return MAPPER.writeValueAsString(payload);
    } catch (RuntimeException e) {
      throw BusinessException.internalError(
              "Failed to serialize saga payload: " + e.getMessage());
    }
  }
}