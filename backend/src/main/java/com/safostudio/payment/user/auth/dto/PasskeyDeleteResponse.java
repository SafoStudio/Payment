package com.safostudio.payment.user.auth.dto;

public record PasskeyDeleteResponse(
        boolean deleted,
        String error
) {
  public static PasskeyDeleteResponse ok() {
    return new PasskeyDeleteResponse(true, null);
  }

  public static PasskeyDeleteResponse error(String message) {
    return new PasskeyDeleteResponse(false, message);
  }
}
