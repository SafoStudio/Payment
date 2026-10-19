package com.safostudio.payment.auth.api;

import com.safostudio.payment.auth.dto.RegisterRequest;
import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.user.domain.Role;
import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.user.domain.UserStatus;
import com.safostudio.payment.user.repository.RoleRepository;
import com.safostudio.payment.user.repository.UserRepository;
import com.safostudio.payment.user.repository.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistrationController {

  private static final String EMAIL = "email";
  private static final String ERROR = "error";
  private static final String EMAIL_ALREADY_REGISTERED = "Email already registered";
  private static final String MERCHANT_ROLE_CODE = "MERCHANT";

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final PasswordEncoder passwordEncoder;
  private final SecurityContextRepository securityContextRepository;

  private final SecurityContextHolderStrategy securityContextHolderStrategy =
          SecurityContextHolder.getContextHolderStrategy();

  @Transactional
  @PostMapping("/register")
  public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request,
                                                      HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {

    if (userRepository.existsByEmail(request.getEmail())) {
      return ResponseEntity.badRequest().body(Map.of(ERROR, EMAIL_ALREADY_REGISTERED));
    }

    Role merchantRole = roleRepository.findByCode(MERCHANT_ROLE_CODE)
            .orElseThrow(() -> BusinessException.invalidRoleCodes(
                    "Role %s is not configured".formatted(MERCHANT_ROLE_CODE)));

    User user = User.builder()
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .status(UserStatus.ACTIVE)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    User saved = userRepository.save(user);
    userRoleRepository.assignRole(saved.getId(), merchantRole.getId());

    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + merchantRole.getCode()));
    var authentication = UsernamePasswordAuthenticationToken.authenticated(
            saved.getEmail(), null, authorities);

    SecurityContext context = securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authentication);
    securityContextHolderStrategy.setContext(context);
    securityContextRepository.saveContext(context, httpRequest, httpResponse);

    log.info("User registered: id={}, email={}, role={}", saved.getId(), saved.getEmail(), merchantRole.getCode());

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                    "userId", saved.getId(),
                    EMAIL, saved.getEmail(),
                    "passkeyEnrollmentRequired", true
            ));
  }
}