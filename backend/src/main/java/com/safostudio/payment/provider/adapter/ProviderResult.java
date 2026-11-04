package com.safostudio.payment.provider.adapter;

import java.util.Map;

public record ProviderResult(
        Status status,
        String providerReference,
        String declineCode,
        Map<String, String> raw
) {
  public enum Status {
    APPROVED,
    DECLINED,
    ERROR
  }

  public ProviderResult {
    if (status == null) {
      throw new IllegalArgumentException("status is required");
    }
    raw = raw == null ? Map.of() : Map.copyOf(raw);
  }

  public static ProviderResult approved(String providerReference, Map<String, String> raw) {
    return new ProviderResult(Status.APPROVED, providerReference, null, raw);
  }

  public static ProviderResult declined(String declineCode, Map<String, String> raw) {
    return new ProviderResult(Status.DECLINED, null, declineCode, raw);
  }

  public static ProviderResult error(String declineCode, Map<String, String> raw) {
    return new ProviderResult(Status.ERROR, null, declineCode, raw);
  }

  public boolean isApproved() {
    return status == Status.APPROVED;
  }
}