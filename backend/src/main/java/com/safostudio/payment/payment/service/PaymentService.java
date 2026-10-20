package com.safostudio.payment.payment.service;

import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.payment.dto.CreatePaymentRequest;
import com.safostudio.payment.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentProcessor paymentProcessor;

  public PaymentResponse createPayment(CreatePaymentRequest request) {
    var existingPayment = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
    if (existingPayment.isPresent()) {
      return PaymentResponse.from(existingPayment.get());
    }

    Payment payment = Payment.create(
            request.getWalletId(),
            request.getTargetWalletId(),
            request.getAmount(),
            request.getCurrency(),
            request.getType(),
            request.getIdempotencyKey(),
            request.getDescription()
    );

    payment = paymentRepository.save(payment);

    try {
      payment = paymentProcessor.process(payment);
    } catch (Exception e) {
      log.error("Processing failed but payment already saved with ID: {}", payment.getId());
    }

    return PaymentResponse.from(payment);
  }

  @Transactional
  public PaymentResponse processPayment(UUID paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> BusinessException.paymentNotFound(paymentId));

    payment = paymentProcessor.process(payment);

    return PaymentResponse.from(payment);
  }

  @Transactional
  public PaymentResponse cancelPayment(UUID paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> BusinessException.paymentNotFound(paymentId));

    payment.cancel();
    payment = paymentRepository.save(payment);

    return PaymentResponse.from(payment);
  }

  @Transactional(readOnly = true)
  public PaymentResponse getPayment(UUID paymentId) {
    return paymentRepository.findById(paymentId)
            .map(PaymentResponse::from)
            .orElseThrow(() -> BusinessException.paymentNotFound(paymentId));
  }

  @Transactional(readOnly = true)
  public Optional<PaymentResponse> getPaymentByIdempotencyKey(String key) {
    return paymentRepository.findByIdempotencyKey(key)
            .map(PaymentResponse::from);
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> getPaymentsByWallet(UUID walletId) {
    return paymentRepository.findByWalletId(walletId)
            .stream()
            .map(PaymentResponse::from)
            .toList();
  }
}