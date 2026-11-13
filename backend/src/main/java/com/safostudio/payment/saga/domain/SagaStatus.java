package com.safostudio.payment.saga.domain;

/**
 * Lifecycle of a saga instance.
 * <p>
 * RUNNING      — saga is executing steps.
 * COMPLETED    — terminal, all steps succeeded.
 * FAILED       — terminal, a step failed and no compensation was needed
 *                (e.g. provider declined before any ledger mutation).
 * COMPENSATING — a step succeeded but a later step failed; compensation is in progress.
 * COMPENSATED  — terminal, compensation finished (ledger rolled back / payment marked FAILED).
 * TIMED_OUT    — terminal, saga exceeded timeout_at while RUNNING.
 * <p>
 * Terminal statuses: COMPLETED, FAILED, COMPENSATED, TIMED_OUT.
 */
public enum SagaStatus {
  RUNNING,
  COMPLETED,
  FAILED,
  COMPENSATING,
  COMPENSATED,
  TIMED_OUT;

  public boolean isTerminal() {
    return this == COMPLETED || this == FAILED || this == COMPENSATED || this == TIMED_OUT;
  }
}