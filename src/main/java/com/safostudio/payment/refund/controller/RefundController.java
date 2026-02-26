package com.safostudio.payment.refund.controller;

import com.safostudio.payment.refund.controller.input.CreateRefundInput;
import com.safostudio.payment.refund.service.RefundService;
import com.safostudio.payment.refund.service.dto.CreateRefundRequest;
import com.safostudio.payment.refund.service.dto.RefundResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @MutationMapping
    public RefundResponse createRefund(@Argument CreateRefundInput input) {
        CreateRefundRequest request = CreateRefundRequest.builder()
                .paymentId(input.paymentId())
                .amount(input.amount())
                .currency(input.currency())
                .reason(input.reason())
                .idempotencyKey(input.idempotencyKey())
                .build();

        return refundService.createRefund(request);
    }

    @MutationMapping
    public RefundResponse processRefund(@Argument UUID id) {
        return refundService.processRefund(id);
    }

    @QueryMapping
    public RefundResponse refund(@Argument UUID id) {
        return refundService.getRefund(id);
    }

    @QueryMapping
    public List<RefundResponse> refundsByPayment(@Argument UUID paymentId) {
        return refundService.getRefundsByPayment(paymentId);
    }
}