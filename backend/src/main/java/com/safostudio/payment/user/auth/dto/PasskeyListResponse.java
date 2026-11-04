package com.safostudio.payment.user.auth.dto;

import java.util.List;

public record PasskeyListResponse(
        List<PasskeyView> items,
        String error
) {
  public static PasskeyListResponse ok(List<PasskeyView> items) {
    return new PasskeyListResponse(items, null);
  }

  public static PasskeyListResponse error(String message) {
    return new PasskeyListResponse(List.of(), message);
  }
}
