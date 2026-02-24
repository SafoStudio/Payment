package com.safostudio.payment.payment.service;

import com.safostudio.payment.payment.domain.Payment;
import com.safostudio.payment.payment.repository.PaymentRepository;
import com.safostudio.payment.wallet.service.TransferService;
import com.safostudio.payment.wallet.service.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessor {

    private final TransferService transferService;
    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment process(Payment payment) {
        try {
            payment.markAsProcessing();

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

        } catch (Exception e) {
            log.error("Payment processing failed: ", e);
            payment.fail();
        }

        return paymentRepository.save(payment);
    }
}