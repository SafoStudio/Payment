package com.safostudio.payment.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LogoutController {

  private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

  private final SecurityContextHolderStrategy securityContextHolderStrategy =
          SecurityContextHolder.getContextHolderStrategy();

  @PostMapping("/logout")
  public ResponseEntity<Map<String, Object>> logout(Authentication authentication,
                                                    HttpServletRequest httpRequest,
                                                    HttpServletResponse httpResponse) {
    if (authentication != null) {
      log.info("User logged out: email={}", authentication.getName());
    }
    logoutHandler.logout(httpRequest, httpResponse, authentication);

    SecurityContext context = securityContextHolderStrategy.createEmptyContext();
    securityContextHolderStrategy.setContext(context);

    return ResponseEntity.ok(Map.of("loggedOut", true));
  }
}