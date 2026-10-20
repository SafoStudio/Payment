package com.safostudio.payment.shared.config;

import com.safostudio.payment.shared.security.AppUserDetailsNormalizingFilter;
import com.safostudio.payment.shared.security.DatabaseUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http,
                                  DatabaseUserDetailsService uds,
                                  SecurityContextRepository securityContextRepository,
                                  PlatformTransactionManager transactionManager) {
    DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider(uds);
    daoProvider.setPasswordEncoder(passwordEncoder());

    AppUserDetailsNormalizingFilter normalizerFilter =
            new AppUserDetailsNormalizingFilter(uds, securityContextRepository);

    WebAuthnTransactionFilter webauthnTxFilter =
            new WebAuthnTransactionFilter(new TransactionTemplate(transactionManager));

    return http
            .securityContext(sc -> sc
                    .securityContextRepository(securityContextRepository))
            .authenticationProvider(daoProvider)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/graphql", "/graphiql/**").permitAll()
                    .requestMatchers("/webauthn/**", "/login/**").permitAll()
                    .anyRequest().authenticated())
            .webAuthn(webAuthn -> webAuthn
                    .rpId("localhost")
                    .rpName("Payment Platform")
                    .allowedOrigins("http://localhost:4200", "http://localhost:8080"))
            .formLogin(withDefaults())
            .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/graphql", "/auth/**"))
            .addFilterBefore(webauthnTxFilter, AuthorizationFilter.class)
            .addFilterBefore(normalizerFilter, AuthorizationFilter.class)
            .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JdbcPublicKeyCredentialUserEntityRepository userEntities(JdbcOperations jdbc) {
    return new JdbcPublicKeyCredentialUserEntityRepository(jdbc);
  }

  @Bean
  JdbcUserCredentialRepository userCredentials(JdbcOperations jdbc) {
    return new JdbcUserCredentialRepository(jdbc);
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }
}