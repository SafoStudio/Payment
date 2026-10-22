package com.safostudio.payment.user.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Wraps /webauthn/** requests in a single transaction.
 * <p>
 * Spring Security WebAuthn repositories (JdbcPublicKeyCredentialUserEntityRepository,
 * JdbcUserCredentialRepository) are not @Transactional. With Hikari auto-commit=false,
 * each statement would be rolled back on connection close, breaking the
 * user_entities -> user_credentials insert sequence (FK violation).
 */
public class WebAuthnTransactionFilter extends OncePerRequestFilter {

  private final TransactionTemplate transactionTemplate;

  public WebAuthnTransactionFilter(TransactionTemplate transactionTemplate) {
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/webauthn/");
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    transactionTemplate.executeWithoutResult(status -> {
      try {
        filterChain.doFilter(request, response);
      } catch (ServletException | IOException e) {
        throw new TransactionSystemException("WebAuthn request failed inside transaction", e);
      }
    });
  }
}