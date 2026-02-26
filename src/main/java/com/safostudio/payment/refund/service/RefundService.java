package com.safostudio.payment.refund.service;

import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.refund.domain.Refund;
import com.safostudio.payment.refund.exception.RefundException;
import com.safostudio.payment.refund.repository.RefundRepository;
import com.safostudio.payment.refund.service.dto.CreateRefundRequest;
import com.safostudio.payment.refund.service.dto.RefundResponse;
import com.safostudio.payment.wallet.domain.IdempotencyKey;
import com.safostudio.payment.wallet.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final IdempotencyRepository idempotencyRepository;

    @Transactional
    public RefundResponse createRefund(CreateRefundRequest request) {

        var existingKey = idempotencyRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingKey.isPresent()) {
            UUID refundId = UUID.fromString(existingKey.get().getResponse());
            return refundRepository.findById(refundId)
                    .map(RefundResponse::from)
                    .orElseThrow(() -> new RuntimeException("Refund not found for idempotency key"));
        }

        var payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + request.getPaymentId()));

        if (!payment.isSucceeded()) {
            throw new RuntimeException("Cannot refund payment with status: " + payment.getStatus());
        }

        if (request.getAmount().compareTo(payment.getAmount()) > 0) {
            throw RefundException.exceedsPaymentAmount(request.getAmount(), payment.getAmount());
        }

        Refund refund = Refund.create(
                request.getPaymentId(),
                request.getAmount(),
                request.getCurrency(),
                request.getReason(),
                payment.getTransactionId()
        );

        refund = refundRepository.save(refund);

        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .response(refund.getId().toString())
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .createdAt(Instant.now())
                .build();

        idempotencyRepository.save(idempotencyKey);

        return RefundResponse.from(refund);
    }

    @Transactional
    public RefundResponse processRefund(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> RefundException.notFound(refundId));

        refund = refundProcessor.process(refund);

        return RefundResponse.from(refund);
    }

    public RefundResponse getRefund(UUID refundId) {
        return refundRepository.findById(refundId)
                .map(RefundResponse::from)
                .orElseThrow(() -> RefundException.notFound(refundId));
    }

    public List<RefundResponse> getRefundsByPayment(UUID paymentId) {
        return refundRepository.findByPaymentId(paymentId)
                .stream()
                .map(RefundResponse::from)
                .collect(Collectors.toList());
    }
}