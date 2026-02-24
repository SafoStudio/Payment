package com.safostudio.payment.payment.controller;

import com.safostudio.payment.payment.controller.input.CreatePaymentInput;
import com.safostudio.payment.payment.domain.PaymentType;
import com.safostudio.payment.payment.service.PaymentService;
import com.safostudio.payment.payment.service.dto.CreatePaymentRequest;
import com.safostudio.payment.payment.service.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @MutationMapping
    public PaymentResponse createPayment(@Argument CreatePaymentInput input) {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .walletId(input.walletId())
                .targetWalletId(input.targetWalletId())
                .amount(input.amount())
                .currency(input.currency())
                .type(PaymentType.TRANSFER)
                .idempotencyKey(input.idempotencyKey())
                .description(input.description())
                .build();

        return paymentService.createPayment(request);
    }

    @MutationMapping
    public PaymentResponse processPayment(@Argument UUID id) {
        return paymentService.processPayment(id);
    }

    @MutationMapping
    public PaymentResponse cancelPayment(@Argument UUID id) {
        return paymentService.cancelPayment(id);
    }

    @QueryMapping
    public PaymentResponse payment(@Argument UUID id) {
        return paymentService.getPayment(id);
    }

    @QueryMapping
    public PaymentResponse paymentByIdempotencyKey(@Argument String key) {
        return paymentService.getPaymentByIdempotencyKey(key);
    }

    @QueryMapping
    public List<PaymentResponse> paymentsByWallet(@Argument UUID walletId) {
        return paymentService.getPaymentsByWallet(walletId);
    }
}