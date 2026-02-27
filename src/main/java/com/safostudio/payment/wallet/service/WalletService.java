package com.safostudio.payment.wallet.service;

import com.safostudio.payment.user.repository.UserRepository;
import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.wallet.domain.Wallet;
import com.safostudio.payment.wallet.exception.WalletServiceException;
import com.safostudio.payment.wallet.repository.WalletRepository;
import com.safostudio.payment.wallet.service.dto.CreateWalletRequest;
import com.safostudio.payment.wallet.service.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.safostudio.payment.wallet.service.dto.TransferRequest;
import com.safostudio.payment.wallet.service.dto.TransactionResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransferService transferService;
    private final UserRepository userRepository;

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        try {
            java.util.Currency.getInstance(request.getCurrency());
        } catch (IllegalArgumentException e) {
            throw WalletServiceException.invalidCurrency(request.getCurrency());
        }

        if (request.getInitialBalance() != null && request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw WalletServiceException.invalidInitialBalance(request.getInitialBalance());
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> WalletServiceException.userNotFound(request.getUserId()));

        if (!user.isActive()) {
            throw WalletServiceException.userNotActive(request.getUserId(), user.getStatus());
        }

        if (walletRepository.existsByUserIdAndCurrency(request.getUserId(), request.getCurrency())) {
            throw WalletServiceException.alreadyExists(request.getUserId(), request.getCurrency());
        }

        Wallet wallet = Wallet.builder()
                .userId(request.getUserId())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .currency(request.getCurrency())
                .status(Wallet.WalletStatus.ACTIVE)
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
    public Wallet getWalletEntity(UUID walletId) {
        return findWalletById(walletId);
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> getWalletsByUserId(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw WalletServiceException.userNotFound(userId);
        }

        return walletRepository.findAllByUserId(userId).stream()
                .map(WalletResponse::fromDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public WalletResponse blockWallet(UUID walletId) {
        Wallet wallet = findWalletById(walletId);

        validateUserForWallet(wallet.getUserId());

        try {
            wallet.block();
        } catch (Exception e) {
            throw WalletServiceException.notActive(walletId);
        }

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

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw WalletServiceException.cannotCloseWithBalance(
                    walletId, wallet.getBalance(), wallet.getCurrency()
            );
        }

        try {
            wallet.close();
        } catch (Exception e) {
            throw WalletServiceException.notActive(walletId);
        }

        Wallet saved = walletRepository.save(wallet);

        return WalletResponse.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public boolean isWalletActive(UUID walletId) {
        return walletRepository.isWalletActive(walletId);
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> getAllWallets() {
        return StreamSupport.stream(walletRepository.findAll().spliterator(), false)
                .map(WalletResponse::fromDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse transferFunds(UUID fromWalletId, UUID toWalletId,
                                             BigDecimal amount, String currency,
                                             String idempotencyKey, String description) {

        Wallet fromWallet = findWalletById(fromWalletId);
        Wallet toWallet = findWalletById(toWalletId);

        validateUserForWallet(fromWallet.getUserId());
        validateUserForWallet(toWallet.getUserId());

        TransferRequest request = TransferRequest.builder()
                .fromWalletId(fromWalletId)
                .toWalletId(toWalletId)
                .amount(amount)
                .currency(currency)
                .idempotencyKey(idempotencyKey)
                .description(description)
                .build();

        return transferService.transfer(request);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceByUserId(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw WalletServiceException.userNotFound(userId);
        }

        return walletRepository.getTotalBalanceByUserId(userId);
    }

    private Wallet findWalletById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> WalletServiceException.notFound(walletId));
    }

    private void validateUserForWallet(UUID userId) {
        if (userId == null) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> WalletServiceException.userNotFound(userId));

        if (!user.isActive()) {
            throw WalletServiceException.userNotActive(userId, user.getStatus());
        }
    }
}