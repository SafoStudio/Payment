package com.safostudio.payment.refund.service;

import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.refund.domain.Refund;
import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.refund.repository.RefundRepository;
import com.safostudio.payment.wallet.service.TransferService;
import com.safostudio.payment.wallet.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundProcessor {

  private final RefundRepository refundRepository;
  private final PaymentRepository paymentRepository;
  private final TransferService transferService;
  private final RefundFailureHandler refundFailureHandler;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Refund process(Refund refund) {
    UUID paymentId = refund.getPaymentId();

    try {
      Payment payment = paymentRepository.findById(paymentId)
              .orElseThrow(() -> BusinessException.paymentNotFound(paymentId));

      if (refund.getAmount().compareTo(payment.getAmount()) > 0) {
        throw BusinessException.refundExceedsPayment(refund.getAmount(), payment.getAmount());
      }

      refund.process();
      refund = refundRepository.save(refund);

      TransferRequest transferRequest = TransferRequest.builder()
              .fromWalletId(payment.getTargetWalletId())
              .toWalletId(payment.getWalletId())
              .amount(refund.getAmount())
              .currency(refund.getCurrency())
              .idempotencyKey("refund-" + refund.getId())
              .description("Refund for payment: " + payment.getId())
              .build();

      var transaction = transferService.transfer(transferRequest);

      refund.complete(transaction.getId());

      payment.refund();
      paymentRepository.save(payment);

      return refundRepository.save(refund);

    } catch (BusinessException e) {
      log.error("Refund failed: {}", e.getMessage());
      refundFailureHandler.markFailed(refund.getId());
      throw e;
    } catch (Exception e) {
      log.error("Refund failed unexpectedly", e);
      refundFailureHandler.markFailed(refund.getId());
      throw BusinessException.internalError("Refund processing failed");
    }
  }
}