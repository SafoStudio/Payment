package com.safostudio.payment.ledger.transaction.api.input;

import com.safostudio.payment.ledger.transaction.domain.TransactionStatus;
import com.safostudio.payment.ledger.transaction.domain.TransactionType;

public record TransactionFilterInput(
        TransactionType type,
        TransactionStatus status,
        String currency
) {}