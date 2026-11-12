package com.safostudio.payment.payment.saga.domain;

import com.safostudio.payment.shared.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted saga instance.
 * <p>
 * One row per (aggregateType, aggregateId, sagaType) — enforced by UNIQUE
 * uq_saga_aggregate. version is an optimistic lock: the same row is updated
 * from the HTTP request thread (createPayment) and from SagaTimeoutPublisher
 * (scheduled). Without @Version a lost update is possible.
 * <p>
 * payload is TEXT (JSON snapshot), not JSONB: V37 established that Spring Data
 * JDBC writes String to jsonb as varchar and fails. Keep it TEXT.
 * <p>
 * Status transitions are encapsulated — callers never assign status directly.
 * Terminal statuses (SagaStatus.isTerminal) cannot transition further, except
 * restartAfterTimeout (TIMED_OUT -> RUNNING), which is the only legal exit
 * from a terminal status and is used by resume.
 * <p>
 * currentStep reflects the last step actually reached. It is not advanced
 * speculatively: a step is marked reached only after its outcome is known.
 * markLedgerReached is called by SagaService on payment SUCCEEDED (ledger is
 * the only path to SUCCEEDED in PaymentProcessor).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("saga_instances")
public class SagaInstance {

  @Id
  private UUID id;

  @Version
  private Long version;

  private String aggregateType;

  private UUID aggregateId;

  private String sagaType;

  private SagaStatus status;

  private SagaStep currentStep;

  private int attempts;

  private Instant nextAttemptAt;

  private Instant timeoutAt;

  private String lastError;

  private String payload;

  private Instant createdAt;

  private Instant updatedAt;

  public static SagaInstance start(String sagaType,
                                   String aggregateType,
                                   UUID aggregateId,
                                   SagaStep initialStep,
                                   String payload,
                                   Instant timeoutAt) {
    Instant now = Instant.now();
    return SagaInstance.builder()
            .id(UUID.randomUUID())
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .sagaType(sagaType)
            .status(SagaStatus.RUNNING)
            .currentStep(initialStep)
            .attempts(0)
            .nextAttemptAt(null)
            .timeoutAt(timeoutAt)
            .lastError(null)
            .payload(payload)
            .createdAt(now)
            .updatedAt(now)
            .build();
  }

  public void advanceTo(SagaStep nextStep, String payload) {
    requireRunning("advance");
    this.currentStep = nextStep;
    this.payload = payload;
    this.attempts = 0;
    this.nextAttemptAt = null;
    this.updatedAt = Instant.now();
  }

  /**
   * Marks LEDGER reached without advancing to a next step. Called from
   * SagaService on payment SUCCEEDED — ledger is the only path to SUCCEEDED
   * in PaymentProcessor, so the fact of SUCCEEDED proves ledger was reached.
   * Keeps attempts untouched (this is not a retry).
   */
  public void markLedgerReached(String payload) {
    requireRunning("mark ledger reached");
    this.currentStep = SagaStep.LEDGER;
    this.payload = payload;
    this.updatedAt = Instant.now();
  }

  public void complete(String payload) {
    requireRunning("complete");
    this.status = SagaStatus.COMPLETED;
    this.payload = payload;
    this.nextAttemptAt = null;
    this.lastError = null;
    this.updatedAt = Instant.now();
  }

  public void fail(SagaStep failedStep, String error, String payload) {
    requireRunning("fail");
    this.status = SagaStatus.FAILED;
    this.currentStep = failedStep;
    this.lastError = truncate(error);
    this.payload = payload;
    this.nextAttemptAt = null;
    this.updatedAt = Instant.now();
  }

  public void beginCompensation(String error) {
    requireRunning("begin compensation");
    this.status = SagaStatus.COMPENSATING;
    this.currentStep = SagaStep.COMPENSATE;
    this.lastError = truncate(error);
    this.nextAttemptAt = null;
    this.updatedAt = Instant.now();
  }

  public void completeCompensation(String payload) {
    if (this.status != SagaStatus.COMPENSATING) {
      throw BusinessException.internalError(
              "Saga %s cannot complete compensation from status %s"
                      .formatted(id, status));
    }
    this.status = SagaStatus.COMPENSATED;
    this.currentStep = SagaStep.COMPENSATE;
    this.payload = payload;
    this.nextAttemptAt = null;
    this.updatedAt = Instant.now();
  }

  public void timeOut(String error) {
    requireRunning("time out");
    this.status = SagaStatus.TIMED_OUT;
    this.lastError = truncate(error);
    this.nextAttemptAt = null;
    this.updatedAt = Instant.now();
  }

  /**
   * Only legal exit from a terminal status: TIMED_OUT -> RUNNING.
   * Used by resume. Resets attempts/next_attempt_at and sets a fresh timeout.
   */
  public void restartAfterTimeout(SagaStep step, Instant newTimeoutAt) {
    if (this.status != SagaStatus.TIMED_OUT) {
      throw BusinessException.internalError(
              "Cannot restart saga %s from status %s".formatted(id, status));
    }
    this.status = SagaStatus.RUNNING;
    this.currentStep = step;
    this.attempts = 0;
    this.nextAttemptAt = null;
    this.timeoutAt = newTimeoutAt;
    this.lastError = null;
    this.updatedAt = Instant.now();
  }

  public boolean isTerminal() {
    return status != null && status.isTerminal();
  }

  private void requireRunning(String action) {
    if (status != SagaStatus.RUNNING) {
      throw BusinessException.internalError(
              "Cannot %s saga %s from status %s".formatted(action, id, status));
    }
  }

  private static String truncate(String error) {
    if (error == null) {
      return null;
    }
    return error.length() <= 2000 ? error : error.substring(0, 2000);
  }
}