package com.safostudio.payment.refund.service;

import com.safostudio.payment.refund.repository.RefundRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundFailureHandler {

  private final RefundRepository refundRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(UUID refundId) {
    refundRepository.findById(refundId).ifPresent(refund -> {
      try {
        refund.fail();
        refundRepository.save(refund);
        log.debug("Refund {} marked as FAILED", refundId);
      } catch (BusinessException e) {
        log.warn("Could not mark refund {} as failed: {}", refundId, e.getMessage());
      }
    });
  }
}