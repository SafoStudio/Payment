package com.safostudio.payment.shared.security;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * UserDetails with the domain user id attached, so the RLS DataSource can read
 * userId from SecurityContext without a DB lookup
 * (avoids the UserDetailsService -> DataSource cycle).
 */
@NullMarked
public class AppUserDetails implements UserDetails {

  @Getter
  private final UUID userId;
  private final String email;
  private final @Nullable String passwordHash;
  private final boolean enabled;
  private final Collection<? extends GrantedAuthority> authorities;

  public AppUserDetails(UUID userId,
                        String email,
                        @Nullable String passwordHash,
                        boolean enabled,
                        Collection<? extends GrantedAuthority> authorities) {
    this.userId = userId;
    this.email = email;
    this.passwordHash = passwordHash;
    this.enabled = enabled;
    this.authorities = authorities;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public @Nullable String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}