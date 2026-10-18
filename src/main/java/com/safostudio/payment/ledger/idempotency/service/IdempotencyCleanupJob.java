package com.safostudio.payment.ledger.idempotency.service;

import com.safostudio.payment.ledger.idempotency.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupJob {

  private final IdempotencyRepository idempotencyRepository;

  @Scheduled(fixedDelayString = "${payment.idempotency.cleanup-interval:PT1H}")
  @Transactional
  public void cleanupExpiredKeys() {
    Instant now = Instant.now();
    idempotencyRepository.deleteExpired(now);
    log.debug("Cleaned up expired idempotency keys at {}", now);
  }
}