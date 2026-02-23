package com.safostudio.payment.wallet.service;

import com.safostudio.payment.wallet.domain.Wallet;
import com.safostudio.payment.wallet.exception.WalletServiceException;
import com.safostudio.payment.wallet.repository.WalletRepository;
import com.safostudio.payment.wallet.service.dto.CreateWalletRequest;
import com.safostudio.payment.wallet.service.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (walletRepository.existsByOwnerIdAndCurrency(request.getOwnerId(), request.getCurrency())) {
            throw WalletServiceException.alreadyExists(request.getOwnerId(), request.getCurrency());
        }

        Wallet wallet = Wallet.builder()
                .ownerId(request.getOwnerId())
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
    public List<WalletResponse> getWalletsByOwner(String ownerId) {
        return walletRepository.findAllByOwnerId(ownerId).stream()
                .map(WalletResponse::fromDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public WalletResponse blockWallet(UUID walletId) {
        Wallet wallet = findWalletById(walletId);

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
        wallet.unblock();

        Wallet saved = walletRepository.save(wallet);
        return WalletResponse.fromDomain(saved);
    }

    @Transactional
    public WalletResponse closeWallet(UUID walletId) {
        Wallet wallet = findWalletById(walletId);

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

    private Wallet findWalletById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> WalletServiceException.notFound(walletId));
    }
}