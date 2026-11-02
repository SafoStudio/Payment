package com.safostudio.payment.ledger.transaction.domain;

import java.util.UUID;

/**
 * Semantic pointer from a ledger transaction to the business entity that produced it.
 * {@code type} names the entity kind, {@code id} is its UUID. Both null means "no reference".
 */
public record TransactionReference(ReferenceType type, UUID id) {

  public TransactionReference {
    if (type == null && id != null) {
      throw new IllegalArgumentException("reference id requires reference type");
    }
  }

  public static TransactionReference payment(UUID paymentId) {
    return new TransactionReference(ReferenceType.PAYMENT, paymentId);
  }

  public static TransactionReference refund(UUID refundId) {
    return new TransactionReference(ReferenceType.REFUND, refundId);
  }

  public static TransactionReference adjustment() {
    return new TransactionReference(ReferenceType.ADJUSTMENT, null);
  }

  public static TransactionReference external(UUID externalId) {
    return new TransactionReference(ReferenceType.EXTERNAL, externalId);
  }

  public static TransactionReference none() {
    return null;
  }
}