package com.safostudio.payment.shared.security;

import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.user.domain.UserStatus;
import com.safostudio.payment.user.repository.RoleRepository;
import com.safostudio.payment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

  private static final String ROLE_PREFIX = "ROLE_";
  private static final String TYPE_ROLE = "ROLE";

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  @Override
  public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    boolean enabled = user.getStatus() == UserStatus.ACTIVE;

    List<GrantedAuthority> authorities = roleRepository.findAuthoritiesByUserId(user.getId())
            .stream()
            .map(row -> new SimpleGrantedAuthority(
                    TYPE_ROLE.equals(row.authorityType())
                            ? ROLE_PREFIX + row.code()
                            : row.code()))
            .map(GrantedAuthority.class::cast)
            .toList();

    return new AppUserDetails(
            user.getId(),
            user.getEmail(),
            user.getPasswordHash() != null ? user.getPasswordHash() : "",
            enabled,
            authorities);
  }
}