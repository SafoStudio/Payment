package com.safostudio.payment.refund.service;

import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.refund.domain.Refund;
import com.safostudio.payment.refund.exception.RefundException;
import com.safostudio.payment.refund.repository.RefundRepository;
import com.safostudio.payment.wallet.service.TransferService;
import com.safostudio.payment.wallet.service.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundProcessor {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final TransferService transferService;

    @Transactional
    public Refund process(Refund refund) {
        UUID paymentId = refund.getPaymentId();

        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

            if (refund.getAmount().compareTo(payment.getAmount()) > 0) {
                throw RefundException.exceedsPaymentAmount(refund.getAmount(), payment.getAmount());
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

        } catch (Exception e) {
            log.error("Refund failed: {}", e.getMessage(), e);
            refund.fail(e.getMessage());
        }

        return refundRepository.save(refund);
    }
}