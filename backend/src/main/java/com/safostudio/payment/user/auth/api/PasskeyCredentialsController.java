package com.safostudio.payment.user.auth.api;

import com.safostudio.payment.user.auth.dto.PasskeyDeleteResponse;
import com.safostudio.payment.user.auth.dto.PasskeyListResponse;
import com.safostudio.payment.user.auth.dto.PasskeyView;
import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/webauthn/credentials")
@RequiredArgsConstructor
public class PasskeyCredentialsController {

  private static final String NOT_AUTHENTICATED = "Not authenticated";
  private static final String USER_NOT_FOUND = "User not found";
  private static final String CREDENTIAL_NOT_FOUND = "Credential not found";
  private static final String FORBIDDEN = "Credential does not belong to current user";
  private static final String LAST_AUTH_METHOD = "Cannot remove the only authentication method";
  private static final String INVALID_CREDENTIAL_ID = "Invalid credential id";

  private final UserRepository userRepository;
  private final PublicKeyCredentialUserEntityRepository userEntities;
  private final UserCredentialRepository userCredentials;

  @GetMapping
  public ResponseEntity<PasskeyListResponse> list(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(PasskeyListResponse.error(NOT_AUTHENTICATED));
    }

    User user = userRepository.findByEmail(authentication.getName()).orElse(null);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(PasskeyListResponse.error(USER_NOT_FOUND));
    }

    PublicKeyCredentialUserEntity userEntity = userEntities.findByUsername(user.getEmail());
    if (userEntity == null) {
      return ResponseEntity.ok(PasskeyListResponse.ok(List.of()));
    }

    List<PasskeyView> items = userCredentials.findByUserId(userEntity.getId()).stream()
            .map(this::toView)
            .toList();

    return ResponseEntity.ok(PasskeyListResponse.ok(items));
  }

  @DeleteMapping("/{credentialId}")
  public ResponseEntity<PasskeyDeleteResponse> delete(
          @PathVariable String credentialId,
          Authentication authentication
  ) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(PasskeyDeleteResponse.error(NOT_AUTHENTICATED));
    }

    User user = userRepository.findByEmail(authentication.getName()).orElse(null);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(PasskeyDeleteResponse.error(USER_NOT_FOUND));
    }

    PublicKeyCredentialUserEntity userEntity = userEntities.findByUsername(user.getEmail());
    if (userEntity == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(PasskeyDeleteResponse.error(CREDENTIAL_NOT_FOUND));
    }

    Bytes credentialBytes = decode(credentialId);
    if (credentialBytes == null) {
      return ResponseEntity.badRequest()
              .body(PasskeyDeleteResponse.error(INVALID_CREDENTIAL_ID));
    }

    CredentialRecord credential = userCredentials.findByCredentialId(credentialBytes);
    if (credential == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(PasskeyDeleteResponse.error(CREDENTIAL_NOT_FOUND));
    }

    if (!userEntity.getId().equals(credential.getUserEntityUserId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(PasskeyDeleteResponse.error(FORBIDDEN));
    }

    List<CredentialRecord> all = userCredentials.findByUserId(userEntity.getId());
    boolean isLast = all.size() == 1;
    boolean noPassword = user.getPasswordHash() == null;
    if (isLast && noPassword) {
      return ResponseEntity.badRequest()
              .body(PasskeyDeleteResponse.error(LAST_AUTH_METHOD));
    }

    userCredentials.delete(credentialBytes);
    return ResponseEntity.ok(PasskeyDeleteResponse.ok());
  }

  private PasskeyView toView(CredentialRecord credential) {
    credential.getLabel();
    return new PasskeyView(
            Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(credential.getCredentialId().getBytes()),
            credential.getLabel(),
            credential.getCreated(),
            credential.getLastUsed()
    );
  }

  private Bytes decode(String credentialId) {
    try {
      return new Bytes(Base64.getUrlDecoder().decode(credentialId));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
