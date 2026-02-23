package com.safostudio.payment.wallet.controller;

import com.safostudio.payment.wallet.controller.input.TopUpInput;
import com.safostudio.payment.wallet.controller.input.TransferInput;
import com.safostudio.payment.wallet.service.TransferService;
import com.safostudio.payment.wallet.service.dto.TopUpRequest;
import com.safostudio.payment.wallet.service.dto.TransactionResponse;
import com.safostudio.payment.wallet.service.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @MutationMapping
    public TransactionResponse transfer(@Argument TransferInput input) {

        TransferRequest request = TransferRequest.builder()
                .fromWalletId(input.fromWalletId())
                .toWalletId(input.toWalletId())
                .amount(input.amount())
                .currency(input.currency())
                .description(input.description())
                .idempotencyKey(input.idempotencyKey())
                .build();

        return transferService.transfer(request);
    }

    @MutationMapping
    public TransactionResponse topUp(@Argument TopUpInput input) {

        TopUpRequest request = TopUpRequest.builder()
                .walletId(input.walletId())
                .amount(input.amount())
                .currency(input.currency())
                .description(input.description())
                .idempotencyKey(input.idempotencyKey())
                .build();

        return transferService.topUp(request);
    }

    @QueryMapping
    public TransactionResponse transaction(@Argument UUID id) {
        return transferService.getTransaction(id);
    }

    @QueryMapping
    public List<TransactionResponse> walletTransactions(@Argument UUID walletId, @Argument int limit) {
        return transferService.getWalletTransactions(walletId, limit);
    }
}