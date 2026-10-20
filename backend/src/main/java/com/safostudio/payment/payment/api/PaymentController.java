package com.safostudio.payment.payment.api;

import com.safostudio.payment.payment.api.input.CreatePaymentInput;
import com.safostudio.payment.payment.domain.PaymentType;
import com.safostudio.payment.payment.service.PaymentService;
import com.safostudio.payment.payment.dto.CreatePaymentRequest;
import com.safostudio.payment.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @PreAuthorize("hasAuthority('payment:create')")
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

  @PreAuthorize("hasAuthority('payment:cancel')")
  @MutationMapping
  public PaymentResponse processPayment(@Argument UUID id) {
    return paymentService.processPayment(id);
  }

  @PreAuthorize("hasAuthority('payment:cancel')")
  @MutationMapping
  public PaymentResponse cancelPayment(@Argument UUID id) {
    return paymentService.cancelPayment(id);
  }

  @PreAuthorize("hasAnyAuthority('payment:read:all', 'payment:read:own')")
  @QueryMapping
  public PaymentResponse payment(@Argument UUID id) {
    return paymentService.getPayment(id);
  }

  @PreAuthorize("hasAnyAuthority('payment:read:all', 'payment:read:own')")
  @QueryMapping
  public Optional<PaymentResponse> paymentByIdempotencyKey(@Argument String key) {
    return paymentService.getPaymentByIdempotencyKey(key);
  }

  @PreAuthorize("hasAnyAuthority('payment:read:all', 'payment:read:own')")
  @QueryMapping
  public List<PaymentResponse> paymentsByWallet(@Argument UUID walletId) {
    return paymentService.getPaymentsByWallet(walletId);
  }
}