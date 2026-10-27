package com.safostudio.payment.payment.service;

import com.safostudio.payment.ledger.idempotency.service.IdempotencyService;
import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.domain.PaymentType;
import com.safostudio.payment.payment.dto.CreatePaymentRequest;
import com.safostudio.payment.payment.dto.PaymentPageResponse;
import com.safostudio.payment.payment.dto.PaymentResponse;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.shared.cursor.CursorCodec;
import com.safostudio.payment.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private static final String OPERATION = "PAYMENT";
  private static final int MAX_LIMIT = 100;
  private static final int DEFAULT_LIMIT = 20;

  private final PaymentRepository paymentRepository;
  private final PaymentProcessor paymentProcessor;
  private final IdempotencyService idempotencyService;

  @Transactional
  public PaymentResponse createPayment(CreatePaymentRequest request) {
    String key = request.getIdempotencyKey();
    String fingerprint = idempotencyService.fingerprint(new PaymentFingerprint(
            request.getWalletId(),
            request.getTargetWalletId(),
            request.getAmount(),
            request.getCurrency(),
            request.getType()
    ));

    IdempotencyService.Result idem = idempotencyService.tryStart(OPERATION, key, fingerprint);
    switch (idem.outcome()) {
      case REPLAY -> {
        return paymentRepository.findByIdempotencyKey(key)
                .map(PaymentResponse::from)
                .orElseThrow(() -> BusinessException.internalError(
                        "Payment not found for idempotency key: " + key));
      }
      case IN_PROGRESS -> throw BusinessException.idempotencyInProgress(key);
      case MISMATCH -> throw BusinessException.idempotencyKeyReuse(key);
      case PROCEED -> { /* continue */ }
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
    payment = paymentProcessor.processNew(payment);

    PaymentResponse response = PaymentResponse.from(payment);
    idempotencyService.complete(OPERATION, key, payment.getId());
    return response;
  }

  public PaymentResponse processPayment(UUID paymentId) {
    Payment payment = paymentProcessor.processExisting(paymentId);
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

  @Transactional(readOnly = true)
  public PaymentPageResponse getPayments(
          String status,
          String type,
          String currency,
          Integer limit,
          String after
  ) {
    int effectiveLimit = normalizeLimit(limit);
    CursorCodec.Cursor cursor = after != null && !after.isBlank()
            ? CursorCodec.decode(after)
            : null;

    Instant cursorTime = cursor != null ? cursor.createdAt() : null;
    UUID cursorId = cursor != null ? cursor.id() : null;

    List<Payment> fetched = paymentRepository.findPage(
            status,
            type,
            currency,
            cursorTime,
            cursorId,
            effectiveLimit + 1
    );

    boolean hasMore = fetched.size() > effectiveLimit;
    List<Payment> page = hasMore ? fetched.subList(0, effectiveLimit) : fetched;

    String nextCursor = null;
    if (hasMore && !page.isEmpty()) {
      Payment last = page.getLast();
      nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
    }

    return new PaymentPageResponse(
            page.stream().map(PaymentResponse::from).toList(),
            nextCursor,
            hasMore
    );
  }

  private int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.clamp(limit, 1, MAX_LIMIT);
  }

  private record PaymentFingerprint(UUID walletId,
                                    UUID targetWalletId,
                                    BigDecimal amount,
                                    String currency,
                                    PaymentType type) {}
}