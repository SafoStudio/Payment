package com.safostudio.payment.fee.service;

import com.safostudio.payment.fee.domain.Fee;
import com.safostudio.payment.fee.dto.FeeResponse;
import com.safostudio.payment.fee.repository.FeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeeService {

  private final FeeRepository feeRepository;

  @Transactional
  public FeeResponse saveFee(Fee fee) {
    Fee saved = feeRepository.save(fee);
    return FeeResponse.from(saved);
  }

  @Transactional
  public void saveAll(List<Fee> fees) {
    feeRepository.saveAll(fees);
  }

  @Transactional(readOnly = true)
  public List<FeeResponse> getFeesByTransaction(UUID transactionId) {
    return feeRepository.findByTransactionId(transactionId)
            .stream()
            .map(FeeResponse::from)
            .toList();
  }

  @Transactional(readOnly = true)
  public List<FeeResponse> getFeesByWallet(UUID walletId) {
    return feeRepository.findByFromWalletId(walletId)
            .stream()
            .map(FeeResponse::from)
            .toList();
  }
}