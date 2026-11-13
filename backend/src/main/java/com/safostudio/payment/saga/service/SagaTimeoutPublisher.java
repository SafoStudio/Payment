package com.safostudio.payment.saga.service;

import com.safostudio.payment.saga.domain.SagaInstance;
import com.safostudio.payment.saga.repository.SagaRepository;
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
 * Dispatch is by saga_type via SagaTimeoutHandler — the publisher holds no
 * aggregate-specific logic. Adding a saga_type means a new handler bean.
 * <p>
 * One bad saga must not abort the batch. Same policy as OutboxPublisher:
 * log and continue; the row will be re-picked on the next poll if still RUNNING.
 * <p>
 * Batch TX (one TX per batch) matches OutboxPublisher; per-event TX is a TD
 * when publisher scaling is introduced.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTimeoutPublisher {

  private final SagaRepository sagaRepository;
  private final List<SagaTimeoutHandler> handlers;

  @Value("${payment.saga.batch-size:50}")
  private int batchSize;

  private Map<String, SagaTimeoutHandler> handlersBySagaType;

  @Scheduled(fixedDelayString = "${payment.saga.poll-interval:PT30S}")
  @Transactional
  public void publish() {
    try (RlsSystemContext ignored = RlsSystemContext.enter()) {
      List<SagaInstance> batch = sagaRepository.findTimedOut(batchSize);
      if (batch.isEmpty()) {
        return;
      }
      Map<String, SagaTimeoutHandler> byType = handlersBySagaType();
      for (SagaInstance saga : batch) {
        dispatch(saga, byType);
      }
    }
  }

  private void dispatch(SagaInstance saga, Map<String, SagaTimeoutHandler> byType) {
    SagaTimeoutHandler handler = byType.get(saga.getSagaType());
    if (handler == null) {
      log.error("No SagaTimeoutHandler for sagaType={} (sagaId={}), leaving RUNNING",
              saga.getSagaType(), saga.getId());
      return;
    }
    try {
      handler.timeOut(saga);
    } catch (Exception e) {
      log.error("Failed to time out saga {} ({}): {}",
              saga.getId(), saga.getSagaType(), e.getMessage(), e);
    }
  }

  private Map<String, SagaTimeoutHandler> handlersBySagaType() {
    if (handlersBySagaType == null) {
      Map<String, SagaTimeoutHandler> map = new HashMap<>();
      for (SagaTimeoutHandler handler : handlers) {
        SagaTimeoutHandler previous = map.put(handler.supportedSagaType(), handler);
        if (previous != null) {
          throw new IllegalStateException(
                  "Duplicate SagaTimeoutHandler for sagaType=" + handler.supportedSagaType());
        }
      }
      handlersBySagaType = Map.copyOf(map);
    }
    return handlersBySagaType;
  }
}