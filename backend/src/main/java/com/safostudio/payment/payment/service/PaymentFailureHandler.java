package com.safostudio.payment.payment.service;

import com.safostudio.payment.payment.repository.PaymentRepository;
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
public class PaymentFailureHandler {

  private final PaymentRepository paymentRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(UUID paymentId) {
    paymentRepository.findById(paymentId).ifPresent(payment -> {
      try {
        payment.fail();
        paymentRepository.save(payment);
        log.debug("Payment {} marked as FAILED", paymentId);
      } catch (BusinessException e) {
        log.warn("Could not mark payment {} as failed: {}", paymentId, e.getMessage());
      }
    });
  }
}