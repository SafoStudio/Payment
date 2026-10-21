package com.safostudio.payment.auth.api;

import com.safostudio.payment.auth.dto.LoginRequest;
import com.safostudio.payment.shared.security.AppUserDetails;
import com.safostudio.payment.user.dto.UserResponse;
import com.safostudio.payment.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

  private static final String ERROR = "error";
  private static final String INVALID_CREDENTIALS = "Invalid email or password";

  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository;
  private final UserService userService;

  private final SecurityContextHolderStrategy securityContextHolderStrategy =
          SecurityContextHolder.getContextHolderStrategy();

  @PostMapping("/login")
  public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse httpResponse) {
    try {
      Authentication authentication = authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                      request.getEmail(), request.getPassword()));

      SecurityContext context = securityContextHolderStrategy.createEmptyContext();
      context.setAuthentication(authentication);
      securityContextHolderStrategy.setContext(context);
      securityContextRepository.saveContext(context, httpRequest, httpResponse);

      AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
      UserResponse me = userService.getCurrentUser();

      assert principal != null;
      log.info("User logged in: id={}, email={}", principal.getUserId(), principal.getUsername());

      return ResponseEntity.ok(Map.of(
              "userId", me.getId(),
              "email", me.getEmail(),
              "roles", me.getRoles(),
              "permissions", me.getPermissions(),
              "hasPasskey", Boolean.TRUE.equals(me.getHasPasskey())
      ));
    } catch (AuthenticationException e) {
      log.warn("Failed login attempt: email={}", request.getEmail());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(Map.of(ERROR, INVALID_CREDENTIALS));
    }
  }
}