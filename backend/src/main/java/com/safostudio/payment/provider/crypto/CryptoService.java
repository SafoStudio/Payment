package com.safostudio.payment.provider.crypto;

import com.safostudio.payment.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CryptoService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;
  private static final int KEY_LENGTH_BYTES = 32;

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public CryptoService(@Value("${payment.provider.credentials-key}") String base64Key) {
    byte[] decoded = Base64.getDecoder().decode(base64Key);
    if (decoded.length != KEY_LENGTH_BYTES) {
      throw new IllegalStateException(
              "payment.provider.credentials-key must be 32 bytes (base64), got " + decoded.length);
    }
    this.key = new SecretKeySpec(decoded, "AES");
  }

  public String encrypt(String plaintext) {
    if (plaintext == null || plaintext.isBlank()) {
      throw BusinessException.validationError("Credential value is empty");
    }
    try {
      byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] combined = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception ex) {
      throw new IllegalStateException("Credential encryption failed", ex);
    }
  }
}