package com.safostudio.payment.payment.saga.service;

import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.payment.saga.domain.SagaInstance;
import com.safostudio.payment.payment.saga.domain.SagaStatus;
import com.safostudio.payment.payment.saga.domain.SagaStep;
import com.safostudio.payment.payment.saga.repository.SagaRepository;
import com.safostudio.payment.payment.service.PaymentProcessor;
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
 * Orchestrates the PAYMENT_SYNC saga.
 * <p>
 * Runs inside the aggregate transaction: PaymentService.createPayment is
 * @Transactional, PaymentProcessor.processNew is REQUIRED — both saga
 * transitions and payment/outbox writes commit together. A separate TX
 * (REQUIRES_NEW) for saga transitions would desync saga vs payment.
 * <p>
 * SagaTimeoutPublisher runs outside the request and uses RlsSystemContext;
 * optimistic lock (@Version) protects against the HTTP thread racing the
 * timeout poller. OptimisticLockingFailureException is not swallowed here —
 * the caller decides (payment already terminal -> surface as-is).
 * <p>
 * payload is a JSON snapshot (TEXT column). It is diagnostic, not a contract;
 * no dedicated DTO. Use tools.jackson, same as OutboxService.
 * <p>
 * currentStep reflects the last step actually reached, not the step we are
 * about to attempt. AUTHORIZE stays set until payment SUCCEEDED proves ledger
 * ran; on SUCCEEDED markLedgerReached sets LEDGER, then complete. A FAILED
 * payment from PaymentProcessor is always an AUTHORIZE-stage failure
 * (NO_PROVIDER / decline / adapter error); ledger failures go through
 * beginCompensation, which sets COMPENSATE.
 * <p>
 * Resume scope on this slice: only PENDING payments. Payment.markAsProcessing
 * requires PENDING and is not idempotent; resuming a PROCESSING payment would
 * need a processor path that skips markAsProcessing (deferred to A, where
 * await_webhook + provider status make the ledger/authorize outcome knowable).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaService {

  public static final String AGGREGATE_TYPE = "PAYMENT";
  public static final String SAGA_TYPE_PAYMENT_SYNC = "PAYMENT_SYNC";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final SagaRepository sagaRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentProcessor paymentProcessor;

  @Value("${payment.saga.timeout:PT2M}")
  private Duration timeout;

  /**
   * Starts a saga for a freshly persisted payment and runs it to a terminal
   * status. Called from PaymentService.createPayment.
   */
  @Transactional
  public Payment runPaymentSaga(Payment payment) {
    SagaInstance saga = SagaInstance.start(
            SAGA_TYPE_PAYMENT_SYNC,
            AGGREGATE_TYPE,
            payment.getId(),
            SagaStep.INITIATE,
            snapshot(payment, null, null),
            Instant.now().plus(timeout)
    );
    saga = sagaRepository.save(saga);

    saga.advanceTo(SagaStep.AUTHORIZE, snapshot(payment, null, null));
    sagaRepository.save(saga);

    return execute(payment, saga);
  }

  /**
   * Resumes an existing saga (processExisting). RUNNING -> continue from
   * current_step; TIMED_OUT -> restart from AUTHORIZE; terminal -> reject
   * (payment is already terminal). Only PENDING payments are resumable on
   * this slice — see class javadoc.
   */
  @Transactional
  public Payment resumePaymentSaga(UUID paymentId) {
    SagaInstance saga = sagaRepository
            .findByAggregateTypeAndAggregateIdAndSagaType(
                    AGGREGATE_TYPE, paymentId, SAGA_TYPE_PAYMENT_SYNC)
            .orElseThrow(() -> BusinessException.internalError(
                    "Saga not found for payment: " + paymentId));

    if (saga.getStatus() == SagaStatus.TIMED_OUT) {
      saga = restartTimedOut(saga);
    } else if (saga.getStatus() != SagaStatus.RUNNING) {
      throw BusinessException.internalError(
              "Cannot resume saga %s in terminal status %s"
                      .formatted(saga.getId(), saga.getStatus()));
    }

    Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> BusinessException.paymentNotFound(paymentId));

    if (!payment.isPending()) {
      throw BusinessException.internalError(
              "Cannot resume payment %s in status %s: processor path for non-PENDING is deferred to A"
                      .formatted(payment.getId(), payment.getStatus()));
    }

    return execute(payment, saga);
  }

  private Payment execute(Payment payment, SagaInstance saga) {
    try {
      Payment processed = paymentProcessor.processNew(payment);

      if (processed.isSucceeded()) {
        String ledgerSnapshot = snapshot(processed, processed.getProviderId(),
                processed.getProviderReference());
        // One transition — one save: LEDGER is committed before COMPLETED, so an
        // exception between the two cannot leave currentStep stale. Cost is one
        // extra UPDATE on success; negligible against the payment/ledger writes.
        saga.markLedgerReached(ledgerSnapshot);
        sagaRepository.save(saga);
        saga.complete(ledgerSnapshot);
        sagaRepository.save(saga);
        log.debug("Saga {} completed for payment {}", saga.getId(), payment.getId());
      } else if (processed.isFailed()) {
        // AUTHORIZE declined / no provider / adapter error: no ledger mutation,
        // no compensation needed. PaymentProcessor already wrote PaymentFailed
        // outbox inside the same TX.
        saga.fail(saga.getCurrentStep(), processed.getFailureReason(),
                snapshot(processed, processed.getProviderId(), null));
        sagaRepository.save(saga);
        log.debug("Saga {} failed for payment {} (reason={})",
                saga.getId(), payment.getId(), processed.getFailureReason());
      } else {
        saga.fail(saga.getCurrentStep(), "UNEXPECTED_PAYMENT_STATUS",
                snapshot(processed, processed.getProviderId(), null));
        sagaRepository.save(saga);
        log.warn("Saga {} unexpected payment status {} for payment {}",
                saga.getId(), processed.getStatus(), payment.getId());
      }
      return processed;
    } catch (BusinessException ledgerFailure) {
      // LEDGER failed after AUTHORIZE approved. PaymentFailureHandler already
      // ran (REQUIRES_NEW) and marked payment FAILED + wrote PaymentFailed
      // outbox. Saga moves to COMPENSATING -> COMPENSATED. We rethrow so the
      // client sees the error, but payment/outbox/saga are committed.
      saga.beginCompensation(ledgerFailure.getMessage());
      sagaRepository.save(saga);

      saga.completeCompensation(snapshot(payment, payment.getProviderId(), null));
      sagaRepository.save(saga);

      log.warn("Saga {} compensated for payment {} (reason={})",
              saga.getId(), payment.getId(), ledgerFailure.getMessage());
      throw ledgerFailure;
    }
  }

  private SagaInstance restartTimedOut(SagaInstance saga) {
    saga.restartAfterTimeout(SagaStep.AUTHORIZE, Instant.now().plus(timeout));
    return sagaRepository.save(saga);
  }

  private String snapshot(Payment payment, UUID providerId, String providerReference) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("paymentId", payment.getId().toString());
    payload.put("walletId", payment.getWalletId() != null ? payment.getWalletId().toString() : null);
    payload.put("targetWalletId", payment.getTargetWalletId().toString());
    payload.put("amount", payment.getAmount().toPlainString());
    payload.put("currency", payment.getCurrency());
    payload.put("method", payment.getMethod());
    payload.put("status", payment.getStatus().name());
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