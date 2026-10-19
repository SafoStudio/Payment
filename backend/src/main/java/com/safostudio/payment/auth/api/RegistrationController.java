package com.safostudio.payment.auth.api;

import com.safostudio.payment.auth.dto.RegisterRequest;
import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.user.domain.UserRole;
import com.safostudio.payment.user.domain.UserStatus;
import com.safostudio.payment.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistrationController {

  private static final String EMAIL = "email";
  private static final String EMAIL_ALREADY_REGISTERED = "Email already registered";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final SecurityContextRepository securityContextRepository;

  private final SecurityContextHolderStrategy securityContextHolderStrategy =
          SecurityContextHolder.getContextHolderStrategy();

  @PostMapping("/register")
  public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request,
                                                      HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {

    if (userRepository.existsByEmail(request.getEmail())) {
      return ResponseEntity.badRequest().body(Map.of("error", EMAIL_ALREADY_REGISTERED));
    }

    User user = User.builder()
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .role(UserRole.MERCHANT)
            .status(UserStatus.ACTIVE)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    User saved = userRepository.save(user);

    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + saved.getRole().name()));
    var authentication = UsernamePasswordAuthenticationToken.authenticated(
            saved.getEmail(), null, authorities);

    SecurityContext context = securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authentication);
    securityContextHolderStrategy.setContext(context);
    securityContextRepository.saveContext(context, httpRequest, httpResponse);

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                    "userId", saved.getId(),
                    EMAIL, saved.getEmail(),
                    "passkeyEnrollmentRequired", true
            ));
  }
}