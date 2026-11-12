package com.safostudio.payment.ledger.outbox.service;

import com.safostudio.payment.ledger.outbox.domain.OutboxEvent;
import com.safostudio.payment.ledger.outbox.repository.OutboxEventRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final OutboxEventRepository outboxEventRepository;

  @Transactional
  public void append(String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload) {
    UUID eventId = UUID.randomUUID();
    String json;
    try {
      json = MAPPER.writeValueAsString(payload);
    } catch (RuntimeException e) {
      throw BusinessException.internalError("Failed to serialize outbox payload: " + e.getMessage());
    }
    OutboxEvent event = OutboxEvent.pending(eventId, aggregateType, aggregateId, eventType, json);
    outboxEventRepository.save(event);
  }
}