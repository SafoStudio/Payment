package com.safostudio.payment.ledger.idempotency.service;

import tools.jackson.databind.json.JsonMapper;
import com.safostudio.payment.ledger.idempotency.domain.IdempotencyKey;
import com.safostudio.payment.ledger.idempotency.repository.IdempotencyRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

  private final IdempotencyRepository idempotencyRepository;
  private final JsonMapper jsonMapper;

  private static final Duration TTL = Duration.ofHours(24);

  public enum Outcome {
    PROCEED,
    REPLAY,
    IN_PROGRESS,
    MISMATCH
  }

  public record Result(Outcome outcome, String response) {}

  @Transactional(propagation = Propagation.MANDATORY)
  public Result tryStart(String operation, String key, String fingerprint) {
    int inserted = idempotencyRepository.tryInsert(
            operation, key, fingerprint, Instant.now().plus(TTL)
    );

    if (inserted == 1) {
      return new Result(Outcome.PROCEED, null);
    }

    IdempotencyKey existing = idempotencyRepository
            .findByOperationAndIdempotencyKey(operation, key)
            .orElseThrow(() -> BusinessException.internalError("Idempotency record vanished after conflict"));

    if (existing.isExpired()) {
      throw BusinessException.validationError("Idempotency key expired: " + key);
    }

    if (existing.getRequestFingerprint() != null
            && !existing.getRequestFingerprint().equals(fingerprint)) {
      return new Result(Outcome.MISMATCH, null);
    }

    if (existing.getStatus() == IdempotencyKey.IdempotencyStatus.PROCESSING) {
      return new Result(Outcome.IN_PROGRESS, null);
    }

    return new Result(Outcome.REPLAY, existing.getResponse());
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void complete(String operation, String key, Object response) {
    try {
      String json = jsonMapper.writeValueAsString(response);
      int updated = idempotencyRepository.complete(operation, key, json);
      if (updated != 1) {
        throw BusinessException.internalError("Failed to complete idempotency record");
      }
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw BusinessException.internalError("Failed to serialize idempotency response");
    }
  }

  public String fingerprint(Object request) {
    try {
      String canonical = jsonMapper.writeValueAsString(request);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw BusinessException.internalError("SHA-256 not available");
    } catch (Exception e) {
      throw BusinessException.internalError("Failed to compute fingerprint");
    }
  }
}