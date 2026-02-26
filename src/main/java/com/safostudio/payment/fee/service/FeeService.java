package com.safostudio.payment.fee.service;

import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.repository.FeeRepository;
import com.safostudio.payment.fee.service.dto.CalculateFeeRequest;
import com.safostudio.payment.fee.service.dto.FeeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeRepository feeRepository;
    private final FeeCalculator feeCalculator;

    /**
     * Calculate and save fees for a transaction
     */
    @Transactional
    public List<FeeResponse> processTransactionFees(UUID transactionId, CalculateFeeRequest request) {
        List<Fee> fees = feeCalculator.calculateFees(request);

        fees.forEach(fee -> {
            // Создаем новый Fee с transactionId
            Fee feeWithTransaction = Fee.builder()
                    .id(fee.getId())
                    .transactionId(transactionId)
                    .fromWalletId(fee.getFromWalletId())
                    .toWalletId(fee.getToWalletId())
                    .amount(fee.getAmount())
                    .currency(fee.getCurrency())
                    .type(fee.getType())
                    .percentage(fee.getPercentage())
                    .calculationRule(fee.getCalculationRule())
                    .createdAt(fee.getCreatedAt())
                    .build();

            feeRepository.save(feeWithTransaction);
        });

        return fees.stream()
                .map(FeeResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public FeeResponse saveFee(Fee fee) {
        Fee saved = feeRepository.save(fee);
        return FeeResponse.from(saved);
    }

    /**
     * Get fees by transaction
     */
    public List<FeeResponse> getFeesByTransaction(UUID transactionId) {
        return feeRepository.findByTransactionId(transactionId)
                .stream()
                .map(FeeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get fees by wallet
     */
    public List<FeeResponse> getFeesByWallet(UUID walletId) {
        return feeRepository.findByFromWalletId(walletId)
                .stream()
                .map(FeeResponse::from)
                .collect(Collectors.toList());
    }
}