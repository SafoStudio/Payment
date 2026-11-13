package com.safostudio.payment.saga.service;

import com.safostudio.payment.saga.domain.SagaInstance;

/**
 * Handles timeout of a single saga instance for one saga_type.
 * <p>
 * Mirrors OutboxEventHandler: the publisher stays generic (poll + dispatch),
 * each saga_type owns its timeout side effects (mark aggregate FAILED, write
 * outbox event). Adding a new saga_type (e.g. A with AWAIT_WEBHOOK) means a
 * new handler, not an if-branch in the publisher.
 * <p>
 * Called inside the publisher transaction with RlsSystemContext already
 * entered. Implementations must be idempotent w.r.t. the aggregate: if the
 * aggregate already reached a terminal state, do not overwrite it.
 */
public interface SagaTimeoutHandler {

  String supportedSagaType();

  void timeOut(SagaInstance saga);
}