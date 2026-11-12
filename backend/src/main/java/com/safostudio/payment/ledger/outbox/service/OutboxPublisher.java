package com.safostudio.payment.ledger.outbox.service;

import com.safostudio.payment.ledger.outbox.domain.OutboxEvent;
import com.safostudio.payment.ledger.outbox.handler.OutboxEventHandler;
import com.safostudio.payment.ledger.outbox.repository.OutboxEventRepository;
import com.safostudio.payment.shared.rls.RlsSystemContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

  private static final int BATCH_SIZE = 50;
  private static final int MAX_ATTEMPTS = 5;
  private static final Duration BASE_BACKOFF = Duration.ofSeconds(5);

  private final OutboxEventRepository outboxEventRepository;
  private final List<OutboxEventHandler> handlers;

  private Map<String, List<OutboxEventHandler>> handlersByEventType;

  @Scheduled(fixedDelayString = "${payment.outbox.poll-interval:PT2S}")
  @Transactional
  public void publish() {
    try (RlsSystemContext ignored = RlsSystemContext.enter()) {
      List<OutboxEvent> batch = outboxEventRepository.findDispatchable(BATCH_SIZE);
      if (batch.isEmpty()) {
        return;
      }
      Map<String, List<OutboxEventHandler>> byType = handlersByEventType();
      for (OutboxEvent event : batch) {
        dispatch(event, byType);
      }
    }
  }

  private void dispatch(OutboxEvent event, Map<String, List<OutboxEventHandler>> byType) {
    List<OutboxEventHandler> eventHandlers = byType.get(event.getEventType());
    if (eventHandlers == null || eventHandlers.isEmpty()) {
      log.warn("No outbox handler for eventType={} (eventId={}), marking DEAD",
              event.getEventType(), event.getEventId());
      event.markDead("No handler registered");
      outboxEventRepository.save(event);
      return;
    }
    try {
      for (OutboxEventHandler handler : eventHandlers) {
        handler.handle(event);
      }
      event.markProcessed();
      outboxEventRepository.save(event);
      log.debug("Outbox event {} ({}) processed by {} handler(s)",
              event.getEventId(), event.getEventType(), eventHandlers.size());
    } catch (Exception e) {
      log.error("Outbox event {} ({}) failed: {}",
              event.getEventId(), event.getEventType(), e.getMessage(), e);
      if (event.getAttempts() + 1 >= MAX_ATTEMPTS) {
        event.markDead(e.getMessage());
      } else {
        Instant next = Instant.now().plus(backoff(event.getAttempts()));
        event.markFailed(e.getMessage(), next);
      }
      outboxEventRepository.save(event);
    }
  }

  private Duration backoff(int attempts) {
    return BASE_BACKOFF.multipliedBy(1L << Math.min(attempts, 6));
  }

  private Map<String, List<OutboxEventHandler>> handlersByEventType() {
    if (handlersByEventType == null) {
      Map<String, List<OutboxEventHandler>> map = new HashMap<>();
      for (OutboxEventHandler handler : handlers) {
        for (String type : handler.supportedEventTypes()) {
          map.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
        }
      }
      Map<String, List<OutboxEventHandler>> copy = new HashMap<>();
      for (Map.Entry<String, List<OutboxEventHandler>> e : map.entrySet()) {
        copy.put(e.getKey(), List.copyOf(e.getValue()));
      }
      handlersByEventType = Map.copyOf(copy);
    }
    return handlersByEventType;
  }
}