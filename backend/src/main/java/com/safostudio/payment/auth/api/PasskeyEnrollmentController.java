package com.safostudio.payment.auth.api;

import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasskeyEnrollmentController {

  private static final String ERROR = "error";
  private static final String NOT_AUTHENTICATED = "Not authenticated";
  private static final String USER_NOT_FOUND = "User not found";
  private static final String NO_PASSKEY_REGISTERED = "No passkey registered";

  private final UserRepository userRepository;
  private final PublicKeyCredentialUserEntityRepository userEntities;
  private final UserCredentialRepository userCredentials;

  @PostMapping("/passkey-enrolled")
  public ResponseEntity<?> passkeyEnrolled(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(401).body(Map.of(ERROR, NOT_AUTHENTICATED));
    }

    String email = authentication.getName();

    User user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
      return ResponseEntity.status(404).body(Map.of(ERROR, USER_NOT_FOUND));
    }

    PublicKeyCredentialUserEntity userEntity = userEntities.findByUsername(email);
    if (userEntity == null) {
      return ResponseEntity.badRequest().body(Map.of(ERROR, NO_PASSKEY_REGISTERED));
    }

    var credentials = userCredentials.findByUserId(userEntity.getId());
    if (credentials.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of(ERROR, NO_PASSKEY_REGISTERED));
    }

    user.updatePasswordHash(null);
    userRepository.save(user);

    return ResponseEntity.ok(Map.of(
            "email", email,
            "passwordCleared", true,
            "credentialsCount", credentials.size()
    ));
  }
}