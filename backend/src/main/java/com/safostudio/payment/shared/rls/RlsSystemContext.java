package com.safostudio.payment.shared.rls;

import com.safostudio.payment.shared.security.AppUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

/**
 * Establishes a synthetic ADMIN identity in SecurityContextHolder for
 * background workers (e.g. OutboxPublisher) that must write to RLS-protected
 * tables without an incoming HTTP request.
 * <p>
 * RlsConnection reads SecurityContextHolder to set app.current_user_id and
 * app.role. Without an identity, RLS policies fail closed (NULL context) and
 * writes are rejected. This helper scopes a system identity to the current
 * thread, restores the previous context on close.
 * <p>
 * Use try-with-resources around the whole unit of work.
 */
public final class RlsSystemContext implements AutoCloseable {

  public static final UUID SYSTEM_USER_ID =
          UUID.fromString("00000000-0000-0000-0000-000000000000");

  private static final String SYSTEM_EMAIL = "system@internal";

  private final SecurityContext previous;

  private RlsSystemContext(SecurityContext previous) {
    this.previous = previous;
  }

  public static RlsSystemContext enter() {
    SecurityContext previous = SecurityContextHolder.getContext();

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    AppUserDetails principal = new AppUserDetails(
            SYSTEM_USER_ID,
            SYSTEM_EMAIL,
            null,
            true,
            authorities
    );

    UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, authorities);

    SecurityContext system = SecurityContextHolder.createEmptyContext();
    system.setAuthentication(authentication);
    SecurityContextHolder.setContext(system);

    return new RlsSystemContext(previous);
  }

  @Override
  public void close() {
    SecurityContextHolder.setContext(previous);
  }
}