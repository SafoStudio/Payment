package com.safostudio.payment.shared.cursor;

import com.safostudio.payment.shared.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class CursorCodec {

  private static final String SEPARATOR = "|";

  private CursorCodec() {}

  public static String encode(Instant createdAt, UUID id) {
    String raw = createdAt.toString() + SEPARATOR + id;
    return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  public static Cursor decode(String cursor) {
    try {
      String raw = new String(
              Base64.getUrlDecoder().decode(cursor),
              StandardCharsets.UTF_8
      );
      int sep = raw.indexOf(SEPARATOR);
      if (sep <= 0) {
        throw new IllegalArgumentException("missing separator");
      }
      Instant createdAt = Instant.parse(raw.substring(0, sep));
      UUID id = UUID.fromString(raw.substring(sep + 1));
      return new Cursor(createdAt, id);
    } catch (RuntimeException e) {
      throw BusinessException.validationError("Invalid cursor");
    }
  }

  public record Cursor(Instant createdAt, UUID id) {}
}