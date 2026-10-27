package com.safostudio.payment.payment.service;

import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.ledger.transaction.dto.TransferRequest;
import com.safostudio.payment.ledger.transaction.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessor {

  private final TransferService transferService;
  private final PaymentRepository paymentRepository;
  private final PaymentFailureHandler paymentFailureHandler;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Payment processNew(Payment payment) {
    try {
      payment.markAsProcessing();
      paymentRepository.save(payment);

      return executeTransfer(payment);

    } catch (BusinessException e) {
      log.error("Payment processing failed: {}", e.getMessage());
      paymentFailureHandler.markFailed(payment.getId());
      throw e;
    } catch (Exception e) {
      log.error("Payment processing failed unexpectedly", e);
      paymentFailureHandler.markFailed(payment.getId());
      throw BusinessException.internalError("Payment processing failed");
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Payment processExisting(UUID paymentId) {
    Payment locked = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> BusinessException.paymentNotFound(paymentId));

    try {
      locked.markAsProcessing();
      paymentRepository.save(locked);

      return executeTransfer(locked);

    } catch (BusinessException e) {
      log.error("Payment processing failed: {}", e.getMessage());
      paymentFailureHandler.markFailed(paymentId);
      throw e;
    } catch (Exception e) {
      log.error("Payment processing failed unexpectedly", e);
      paymentFailureHandler.markFailed(paymentId);
      throw BusinessException.internalError("Payment processing failed");
    }
  }

  private Payment executeTransfer(Payment payment) {
    TransferRequest request = TransferRequest.builder()
            .fromWalletId(payment.getWalletId())
            .toWalletId(payment.getTargetWalletId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .idempotencyKey(payment.getIdempotencyKey())
            .description(payment.getDescription())
            .build();

    var transaction = transferService.transfer(request);
    payment.succeed(transaction.getId());

    return paymentRepository.save(payment);
  }
}