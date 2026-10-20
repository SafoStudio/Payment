package com.safostudio.payment.refund.service;

import tools.jackson.databind.json.JsonMapper;
import com.safostudio.payment.ledger.idempotency.service.IdempotencyService;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.refund.domain.Refund;
import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.refund.repository.RefundRepository;
import com.safostudio.payment.refund.dto.CreateRefundRequest;
import com.safostudio.payment.refund.dto.RefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

  private final RefundRepository refundRepository;
  private final PaymentRepository paymentRepository;
  private final RefundProcessor refundProcessor;
  private final IdempotencyService idempotencyService;
  private final JsonMapper jsonMapper;

  @Transactional
  public RefundResponse createRefund(CreateRefundRequest request) {
    var payment = paymentRepository.findById(request.getPaymentId())
            .orElseThrow(() -> BusinessException.paymentNotFound(request.getPaymentId()));

    if (!payment.isSucceeded()) {
      throw BusinessException.paymentCannotRefund(payment.getStatus());
    }

    if (request.getAmount().compareTo(payment.getAmount()) > 0) {
      throw BusinessException.refundExceedsPayment(request.getAmount(), payment.getAmount());
    }

    String operation = "REFUND";
    String key = request.getIdempotencyKey();
    String fingerprint = idempotencyService.fingerprint(new RefundFingerprint(
            request.getPaymentId(),
            request.getAmount(),
            request.getCurrency()
    ));

    IdempotencyService.Result idem = idempotencyService.tryStart(operation, key, fingerprint);
    switch (idem.outcome()) {
      case REPLAY -> {
        return deserialize(idem.response(), RefundResponse.class);
      }
      case IN_PROGRESS -> throw BusinessException.idempotencyInProgress(key);
      case MISMATCH -> throw BusinessException.idempotencyKeyReuse(key);
      case PROCEED -> { /* continue */ }
    }

    Refund refund = Refund.create(
            request.getPaymentId(),
            request.getAmount(),
            request.getCurrency(),
            request.getReason(),
            payment.getTransactionId()
    );

    refund = refundRepository.save(refund);

    RefundResponse response = RefundResponse.from(refund);
    idempotencyService.complete(operation, key, response);
    return response;
  }

  @Transactional
  public RefundResponse processRefund(UUID refundId) {
    Refund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> BusinessException.refundNotFound(refundId));

    refund = refundProcessor.process(refund);

    return RefundResponse.from(refund);
  }

  @Transactional(readOnly = true)
  public RefundResponse getRefund(UUID refundId) {
    return refundRepository.findById(refundId)
            .map(RefundResponse::from)
            .orElseThrow(() -> BusinessException.refundNotFound(refundId));
  }

  @Transactional(readOnly = true)
  public List<RefundResponse> getRefundsByPayment(UUID paymentId) {
    return refundRepository.findByPaymentId(paymentId)
            .stream()
            .map(RefundResponse::from)
            .toList();
  }

  private <T> T deserialize(String json, Class<T> type) {
    try {
      return jsonMapper.readValue(json, type);
    } catch (Exception e) {
      throw BusinessException.internalError("Failed to deserialize idempotent response");
    }
  }

  private record RefundFingerprint(UUID paymentId, BigDecimal amount, String currency) {}
}