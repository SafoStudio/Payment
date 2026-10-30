package com.safostudio.payment.user.auth.api;

import com.safostudio.payment.user.auth.dto.RegisterRequest;
import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.shared.security.AppUserDetails;
import com.safostudio.payment.user.domain.Role;
import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.user.domain.UserStatus;
import com.safostudio.payment.user.repository.RoleRepository;
import com.safostudio.payment.user.repository.UserRepository;
import com.safostudio.payment.user.repository.UserRoleRepository;
import com.safostudio.payment.wallet.domain.Wallet;
import com.safostudio.payment.wallet.domain.WalletStatus;
import com.safostudio.payment.wallet.repository.WalletRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistrationController {

  private static final String EMAIL = "email";
  private static final String ERROR = "error";
  private static final String EMAIL_ALREADY_REGISTERED = "Email already registered";
  private static final String MERCHANT_ROLE_CODE = "MERCHANT";
  private static final String DEFAULT_CURRENCY = "USD";
  private static final String ROLE_PREFIX = "ROLE_";
  private static final String AUTHORITY_TYPE_ROLE = "ROLE";

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final WalletRepository walletRepository;
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

    List<GrantedAuthority> authorities = roleRepository.findAuthoritiesByUserId(saved.getId())
            .stream()
            .map(row -> new SimpleGrantedAuthority(
                    AUTHORITY_TYPE_ROLE.equals(row.authorityType())
                            ? ROLE_PREFIX + row.code()
                            : row.code()))
            .map(GrantedAuthority.class::cast)
            .toList();

    AppUserDetails principal = new AppUserDetails(
            saved.getId(),
            saved.getEmail(),
            null,
            true,
            authorities
    );

    var authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal, null, authorities);

    SecurityContext context = securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authentication);
    securityContextHolderStrategy.setContext(context);
    securityContextRepository.saveContext(context, httpRequest, httpResponse);

    Wallet wallet = Wallet.builder()
            .userId(saved.getId())
            .balance(BigDecimal.ZERO)
            .currency(DEFAULT_CURRENCY)
            .status(WalletStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    walletRepository.save(wallet);

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                    "userId", saved.getId(),
                    EMAIL, saved.getEmail(),
                    "passkeyEnrollmentRequired", true
            ));
  }
}