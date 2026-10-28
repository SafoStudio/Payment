package com.safostudio.payment.refund.api;

import com.safostudio.payment.refund.api.input.CreateRefundInput;
import com.safostudio.payment.refund.api.input.RefundFilterInput;
import com.safostudio.payment.refund.dto.RefundPageResponse;
import com.safostudio.payment.refund.service.RefundQueryService;
import com.safostudio.payment.refund.service.RefundService;
import com.safostudio.payment.refund.dto.CreateRefundRequest;
import com.safostudio.payment.refund.dto.RefundResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class RefundController {

  private final RefundService refundService;
  private final RefundQueryService refundQueryService;

  @PreAuthorize("hasAuthority('refund:create')")
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

  @PreAuthorize("hasAuthority('refund:create')")
  @MutationMapping
  public RefundResponse processRefund(@Argument UUID id) {
    return refundService.processRefund(id);
  }

  @PreAuthorize("hasAnyAuthority('refund:read:all', 'refund:read:own')")
  @QueryMapping
  public RefundResponse refund(@Argument UUID id) {
    return refundService.getRefund(id);
  }

  @PreAuthorize("hasAnyAuthority('refund:read:all', 'refund:read:own')")
  @QueryMapping
  public List<RefundResponse> refundsByPayment(@Argument UUID paymentId) {
    return refundService.getRefundsByPayment(paymentId);
  }

  @PreAuthorize("hasAnyAuthority('refund:read:all', 'refund:read:own')")
  @QueryMapping
  public RefundPageResponse refunds(
          @Argument RefundFilterInput filter,
          @Argument Integer limit,
          @Argument String after
  ) {
    String status = filter != null && filter.status() != null
            ? filter.status().name()
            : null;
    String currency = filter != null ? filter.currency() : null;

    return refundQueryService.getRefunds(status, currency, limit, after);
  }
}