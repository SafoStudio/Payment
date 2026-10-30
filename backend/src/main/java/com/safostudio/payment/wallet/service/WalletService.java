package com.safostudio.payment.wallet.service;

import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.shared.security.AppUserDetails;
import com.safostudio.payment.shared.util.MoneyUtils;
import com.safostudio.payment.user.repository.UserRepository;
import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.wallet.domain.Wallet;
import com.safostudio.payment.wallet.domain.WalletStatus;
import com.safostudio.payment.wallet.repository.WalletRepository;
import com.safostudio.payment.wallet.dto.CreateWalletRequest;
import com.safostudio.payment.wallet.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

  private static final String ADMIN_ROLE = "ROLE_ADMIN";

  private final WalletRepository walletRepository;
  private final UserRepository userRepository;

  @Transactional
  public WalletResponse createWallet(CreateWalletRequest request) {
    MoneyUtils.validateCurrency(request.getCurrency());

    if (!currentUserIsAdmin()) {
      UUID currentUserId = currentUserIdOrNull();
      if (currentUserId == null || !currentUserId.equals(request.getUserId())) {
        throw BusinessException.validationError("Cannot create a wallet for another user");
      }
    }

    User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> BusinessException.userNotFound(request.getUserId()));

    if (!user.isActive()) {
      throw BusinessException.userNotActive(request.getUserId(), user.getStatus());
    }

    if (walletRepository.existsByUserIdAndCurrency(request.getUserId(), request.getCurrency())) {
      throw BusinessException.walletAlreadyExists(request.getUserId(), request.getCurrency());
    }

    Wallet wallet = Wallet.builder()
            .userId(request.getUserId())
            .balance(BigDecimal.ZERO)
            .currency(request.getCurrency())
            .status(WalletStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    Wallet saved = walletRepository.save(wallet);
    return WalletResponse.fromDomain(saved);
  }

  @Transactional(readOnly = true)
  public WalletResponse getWallet(UUID walletId) {
    Wallet wallet = findWalletById(walletId);
    return WalletResponse.fromDomain(wallet);
  }

  @Transactional(readOnly = true)
  public List<WalletResponse> getWalletsByUserId(UUID userId) {
    if (!userRepository.existsById(userId)) {
      throw BusinessException.userNotFound(userId);
    }

    return walletRepository.findAllByUserId(userId).stream()
            .map(WalletResponse::fromDomain)
            .toList();
  }

  @Transactional
  public WalletResponse blockWallet(UUID walletId) {
    Wallet wallet = findWalletById(walletId);

    validateUserForWallet(wallet.getUserId());

    wallet.block();

    Wallet saved = walletRepository.save(wallet);

    return WalletResponse.fromDomain(saved);
  }

  @Transactional
  public WalletResponse unblockWallet(UUID walletId) {
    Wallet wallet = findWalletById(walletId);

    validateUserForWallet(wallet.getUserId());

    wallet.unblock();

    Wallet saved = walletRepository.save(wallet);

    return WalletResponse.fromDomain(saved);
  }

  @Transactional
  public WalletResponse closeWallet(UUID walletId) {
    Wallet wallet = findWalletById(walletId);

    validateUserForWallet(wallet.getUserId());

    wallet.close();

    Wallet saved = walletRepository.save(wallet);

    return WalletResponse.fromDomain(saved);
  }

  @Transactional(readOnly = true)
  public List<WalletResponse> getAllWallets() {
    return walletRepository.findAllUserWallets().stream()
            .map(WalletResponse::fromDomain)
            .toList();
  }

  private Wallet findWalletById(UUID walletId) {
    return walletRepository.findById(walletId)
            .orElseThrow(() -> BusinessException.walletNotFound(walletId));
  }

  private void validateUserForWallet(UUID userId) {
    if (userId == null) {
      log.debug("Skipping user validation for system wallet (userId is null)");
      return;
    }

    User user = userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.userNotFound(userId));

    if (!user.isActive()) {
      throw BusinessException.userNotActive(userId, user.getStatus());
    }
  }

  private boolean currentUserIsAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;
    for (GrantedAuthority a : auth.getAuthorities()) {
      if (ADMIN_ROLE.equals(a.getAuthority())) return true;
    }
    return false;
  }

  private UUID currentUserIdOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails principal)) {
      return null;
    }
    return principal.getUserId();
  }
}