package com.safostudio.payment.ledger.outbox.handler;

import com.safostudio.payment.ledger.audit.domain.AuditLog;
import com.safostudio.payment.ledger.audit.repository.AuditLogRepository;
import com.safostudio.payment.ledger.outbox.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditOutboxHandler implements OutboxEventHandler {

  private final AuditLogRepository auditLogRepository;

  @Override
  public List<String> supportedEventTypes() {
    return List.of("PaymentSucceeded", "PaymentFailed", "RefundCompleted", "RefundFailed");
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(OutboxEvent event) {
    try {
      auditLogRepository.save(AuditLog.create(
              event.getAggregateType(),
              event.getAggregateId(),
              event.getEventType(),
              null,
              null,
              event.getPayload(),
              null,
              null,
              event.getEventId()
      ));
    } catch (DuplicateKeyException e) {
      log.debug("Audit for outbox event {} already exists, skipping", event.getEventId());
    }
  }
}