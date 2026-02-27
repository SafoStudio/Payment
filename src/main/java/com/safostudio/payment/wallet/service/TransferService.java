package com.safostudio.payment.wallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.service.FeeCalculator;
import com.safostudio.payment.fee.service.FeeService;
import com.safostudio.payment.fee.service.dto.CalculateFeeRequest;
import com.safostudio.payment.wallet.exception.TransferServiceException;
import com.safostudio.payment.wallet.util.MoneyUtils;
import com.safostudio.payment.wallet.domain.IdempotencyKey;
import com.safostudio.payment.wallet.domain.Transaction;
import com.safostudio.payment.wallet.domain.Wallet;
import com.safostudio.payment.wallet.repository.IdempotencyRepository;
import com.safostudio.payment.wallet.repository.TransactionRepository;
import com.safostudio.payment.wallet.repository.WalletRepository;
import com.safostudio.payment.wallet.service.dto.TopUpRequest;
import com.safostudio.payment.wallet.service.dto.TransactionResponse;
import com.safostudio.payment.wallet.service.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final FeeCalculator feeCalculator;
    private final FeeService feeService;

    private static final UUID SYSTEM_REVENUE_WALLET =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SYSTEM_FEES_WALLET =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {

        if (idempotencyRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            throw TransferServiceException.duplicateIdempotencyKey(request.getIdempotencyKey());
        }

        validateTransferRequest(request);

        Wallet fromWallet = walletRepository.findById(request.getFromWalletId())
                .orElseThrow(() -> TransferServiceException.walletNotFound(request.getFromWalletId()));

        Wallet toWallet = walletRepository.findById(request.getToWalletId())
                .orElseThrow(() -> TransferServiceException.walletNotFound(request.getToWalletId()));

        validateWalletsForTransfer(fromWallet, toWallet, request);

        Transaction transaction = Transaction.createTransfer(
                fromWallet.getId(),
                toWallet.getId(),
                request.getAmount(),
                request.getCurrency(),
                request.getDescription()
        );

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            fromWallet.debit(request.getAmount());
            toWallet.credit(request.getAmount());

            walletRepository.save(fromWallet);
            walletRepository.save(toWallet);

            savedTransaction.complete();
            transactionRepository.save(savedTransaction);

            // Calculate and save fees
            try {
                CalculateFeeRequest feeRequest = CalculateFeeRequest.builder()
                        .transactionType("TRANSFER")
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .fromWalletId(request.getFromWalletId())
                        .toWalletId(request.getToWalletId())
                        .build();

                List<Fee> fees = feeCalculator.calculateFees(feeRequest);

                for (Fee fee : fees) {
                    Fee feeWithTransaction = Fee.builder()
                            .transactionId(savedTransaction.getId())
                            .fromWalletId(fee.getFromWalletId())
                            .toWalletId(fee.getToWalletId())
                            .amount(fee.getAmount())
                            .currency(fee.getCurrency())
                            .type(fee.getType())
                            .percentage(fee.getPercentage())
                            .calculationRule(fee.getCalculationRule())
                            .createdAt(Instant.now())
                            .build();

                    feeService.saveFee(feeWithTransaction);

                    if (fee.getFromWalletId() != null) {
                        Wallet feeWallet = walletRepository.findById(fee.getFromWalletId())
                                .orElseThrow(() -> TransferServiceException.walletNotFound(fee.getFromWalletId()));
                        feeWallet.debit(fee.getAmount());
                        walletRepository.save(feeWallet);
                    }

                    if (fee.getToWalletId() != null) {
                        Wallet feeToWallet = walletRepository.findById(fee.getToWalletId())
                                .orElseThrow(() -> TransferServiceException.walletNotFound(fee.getToWalletId()));
                        feeToWallet.credit(fee.getAmount());
                        walletRepository.save(feeToWallet);
                    }

                    if (fee.getFromWalletId() != null && fee.getToWalletId() != null) {
                        Transaction feeTransaction = Transaction.createTransfer(
                                fee.getFromWalletId(),
                                fee.getToWalletId(),
                                fee.getAmount(),
                                fee.getCurrency(),
                                "Fee for transaction: " + savedTransaction.getId()
                        );
                    feeTransaction.complete();
                    transactionRepository.save(feeTransaction);
                    }
                }

            } catch (Exception e) {
                log.error("Failed to process fees for transaction: {}", savedTransaction.getId(), e);
            }

            saveIdempotencyKey(request.getIdempotencyKey(), savedTransaction);

            return TransactionResponse.fromDomain(savedTransaction);

        } catch (Exception e) {
            savedTransaction.fail();
            transactionRepository.save(savedTransaction);

            if (e instanceof IllegalArgumentException) {
                throw TransferServiceException.invalidAmount();
            }
            throw TransferServiceException.transactionFailed(savedTransaction.getId());
        }
    }

    @Transactional
    public TransactionResponse topUp(TopUpRequest request) {
        if (idempotencyRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            throw TransferServiceException.duplicateIdempotencyKey(request.getIdempotencyKey());
        }

        MoneyUtils.validateAmount(request.getAmount());
        MoneyUtils.validateCurrency(request.getCurrency());

        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> TransferServiceException.walletNotFound(request.getWalletId()));

        if (!wallet.getCurrency().equals(request.getCurrency())) {
            throw TransferServiceException.currencyMismatch(wallet.getCurrency(), request.getCurrency());
        }
        if (!wallet.isActive()) {
            throw TransferServiceException.walletNotActive(wallet.getId());
        }

        Transaction transaction = Transaction.createTopUp(
                wallet.getId(),
                request.getAmount(),
                request.getCurrency(),
                request.getDescription()
        );

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            wallet.credit(request.getAmount());
            walletRepository.save(wallet);

            savedTransaction.complete();
            transactionRepository.save(savedTransaction);

            saveIdempotencyKey(request.getIdempotencyKey(), savedTransaction);

            return TransactionResponse.fromDomain(savedTransaction);

        } catch (Exception e) {
            savedTransaction.fail();
            transactionRepository.save(savedTransaction);
            throw TransferServiceException.transactionFailed(savedTransaction.getId());
        }
    }

    private void validateTransferRequest(TransferRequest request) {
        MoneyUtils.validateAmount(request.getAmount());
        MoneyUtils.validateCurrency(request.getCurrency());

        if (request.getFromWalletId().equals(request.getToWalletId())) {
            throw TransferServiceException.sameWallet();
        }
    }

    private void validateWalletsForTransfer(Wallet fromWallet, Wallet toWallet, TransferRequest request) {
        if (!fromWallet.getCurrency().equals(request.getCurrency())) {
            throw TransferServiceException.currencyMismatch(fromWallet.getCurrency(), request.getCurrency());
        }
        if (!toWallet.getCurrency().equals(request.getCurrency())) {
            throw TransferServiceException.currencyMismatch(toWallet.getCurrency(), request.getCurrency());
        }
        if (!fromWallet.isActive()) {
            throw TransferServiceException.walletNotActive(fromWallet.getId());
        }
        if (!toWallet.isActive()) {
            throw TransferServiceException.walletNotActive(toWallet.getId());
        }

        if (fromWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw TransferServiceException.insufficientFunds(
                    fromWallet.getId(),
                    fromWallet.getBalance(),
                    request.getAmount(),
                    request.getCurrency()
            );
        }
    }

    private void saveIdempotencyKey(String key, Transaction transaction) {
        try {
            IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                    .id(null)
                    .idempotencyKey(key)
                    .response(objectMapper.writeValueAsString(TransactionResponse.fromDomain(transaction)))
                    .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                    .createdAt(Instant.now())
                    .build();

            idempotencyRepository.save(idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to save idempotency key", e);
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> TransferServiceException.transactionNotFound(transactionId));

        return TransactionResponse.fromDomain(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getWalletTransactions(UUID walletId, int limit) {
        if (!walletRepository.existsById(walletId)) {
            throw TransferServiceException.walletNotFound(walletId);
        }

        return transactionRepository.findRecentTransactions(walletId, limit).stream()
                .map(TransactionResponse::fromDomain)
                .collect(Collectors.toList());
    }
}