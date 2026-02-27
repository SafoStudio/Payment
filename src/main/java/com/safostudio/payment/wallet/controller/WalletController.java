package com.safostudio.payment.wallet.controller;

import com.safostudio.payment.wallet.service.WalletService;
import com.safostudio.payment.wallet.service.dto.CreateWalletRequest;
import com.safostudio.payment.wallet.service.dto.WalletResponse;
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
public class WalletController {

    private final WalletService walletService;

    @QueryMapping
    public WalletResponse wallet(@Argument UUID id) {
        return walletService.getWallet(id);
    }

    @QueryMapping
    public List<WalletResponse> walletsByUserId(@Argument UUID userId) {
        return walletService.getWalletsByUserId(userId);
    }

    @QueryMapping
    public List<WalletResponse> wallets() {
        return walletService.getAllWallets();
    }

    @MutationMapping
    public WalletResponse createWallet(@Argument CreateWalletRequest input) {
        return walletService.createWallet(input);
    }

    @MutationMapping
    public WalletResponse blockWallet(@Argument UUID id) {
        return walletService.blockWallet(id);
    }

    @MutationMapping
    public WalletResponse unblockWallet(@Argument UUID id) {
        return walletService.unblockWallet(id);
    }

    @MutationMapping
    public WalletResponse closeWallet(@Argument UUID id) {
        return walletService.closeWallet(id);
    }
}