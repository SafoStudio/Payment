package com.safostudio.payment.wallet.api;

import com.safostudio.payment.wallet.service.WalletService;
import com.safostudio.payment.wallet.dto.CreateWalletRequest;
import com.safostudio.payment.wallet.dto.WalletResponse;
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
public class WalletController {

  private final WalletService walletService;

  @PreAuthorize("hasAnyAuthority('wallet:read:all', 'wallet:read:own')")
  @QueryMapping
  public WalletResponse wallet(@Argument UUID id) {
    return walletService.getWallet(id);
  }

  @PreAuthorize("hasAuthority('wallet:read:all')")
  @QueryMapping
  public List<WalletResponse> walletsByUserId(@Argument UUID userId) {
    return walletService.getWalletsByUserId(userId);
  }

  @PreAuthorize("hasAnyAuthority('wallet:read:all', 'wallet:read:own')")
  @QueryMapping
  public List<WalletResponse> wallets() {
    return walletService.getAllWallets();
  }

  @PreAuthorize("hasAuthority('wallet:create')")
  @MutationMapping
  public WalletResponse createWallet(@Argument CreateWalletRequest input) {
    return walletService.createWallet(input);
  }

  @PreAuthorize("hasAuthority('wallet:block')")
  @MutationMapping
  public WalletResponse blockWallet(@Argument UUID id) {
    return walletService.blockWallet(id);
  }

  @PreAuthorize("hasAuthority('wallet:block')")
  @MutationMapping
  public WalletResponse unblockWallet(@Argument UUID id) {
    return walletService.unblockWallet(id);
  }

  @PreAuthorize("hasAuthority('wallet:block')")
  @MutationMapping
  public WalletResponse closeWallet(@Argument UUID id) {
    return walletService.closeWallet(id);
  }
}