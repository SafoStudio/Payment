package com.safostudio.payment.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Normalizes the authenticated principal to AppUserDetails on every request.
 * <p>
 * Spring Security WebAuthn sets the principal to ImmutablePublicKeyCredentialUserEntity
 * (opaque user handle) after passkey login. Downstream code (RLS DataSource, services)
 * expects AppUserDetails (it carries userId). This filter replaces the Authentication
 * with a UsernamePasswordAuthenticationToken backed by AppUserDetails and persists it
 * in the SecurityContextRepository (session), so subsequent requests use AppUserDetails.
 * <p>
 * Idempotent: if the principal is already AppUserDetails, nothing happens.
 */
public class AppUserDetailsNormalizingFilter extends OncePerRequestFilter {

  private final DatabaseUserDetailsService userDetailsService;
  private final SecurityContextRepository securityContextRepository;

  public AppUserDetailsNormalizingFilter(DatabaseUserDetailsService userDetailsService,
                                         SecurityContextRepository securityContextRepository) {
    this.userDetailsService = userDetailsService;
    this.securityContextRepository = securityContextRepository;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof WebAuthnAuthentication webAuthnAuth
            && auth.isAuthenticated()
            && !(auth.getPrincipal() instanceof AppUserDetails)) {
      try {
        AppUserDetails appUser = (AppUserDetails) userDetailsService.loadUserByUsername(webAuthnAuth.getName());
        UsernamePasswordAuthenticationToken replacement =
                UsernamePasswordAuthenticationToken.authenticated(
                        appUser,
                        null,
                        appUser.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(replacement);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
      } catch (UsernameNotFoundException ignored) {
        // No matching local user: leave as-is; downstream RLS fails closed.
      }
    }
    filterChain.doFilter(request, response);
  }
}