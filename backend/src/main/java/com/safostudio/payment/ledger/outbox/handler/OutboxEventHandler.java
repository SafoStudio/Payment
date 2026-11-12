package com.safostudio.payment.ledger.outbox.handler;

import com.safostudio.payment.ledger.outbox.domain.OutboxEvent;

import java.util.List;

public interface OutboxEventHandler {

  List<String> supportedEventTypes();

  void handle(OutboxEvent event);
}