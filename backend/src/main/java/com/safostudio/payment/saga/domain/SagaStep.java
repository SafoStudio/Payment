package com.safostudio.payment.saga.domain;

/**
 * Steps of the PAYMENT_SYNC saga.
 * <p>
 * INITIATE  — records the payment snapshot in payload; no external call.
 * AUTHORIZE — routing + provider authorize (PaymentProcessor).
 * LEDGER    — ledger transfer + payment.succeed (PaymentProcessor).
 * COMPENSATE— compensation branch when LEDGER fails after AUTHORIZE succeeded.
 * <p>
 * NOTIFY is intentionally not a step: outbox events (PaymentSucceeded /
 * PaymentFailed) are written inside PaymentProcessor / PaymentFailureHandler
 * in the same transaction as the aggregate change. A separate NOTIFY step
 * would duplicate outbox writes and break exactly-once on handlers.
 * <p>
 * A will add AWAIT_WEBHOOK as a new step; the set is open for extension.
 */
public enum SagaStep {
  INITIATE,
  AUTHORIZE,
  LEDGER,
  COMPENSATE
}