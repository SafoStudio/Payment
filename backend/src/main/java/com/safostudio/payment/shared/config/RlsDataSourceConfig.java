package com.safostudio.payment.shared.config;

import com.safostudio.payment.shared.rls.RlsDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Primary DataSource is the RLS-aware wrapper around Hikari.
 * <p>
 * Only the RLS wrapper is @Primary. Hikari stays a plain bean
 * so Spring Boot's JdbcTemplate / DataSourceTransactionManager auto-config
 * see exactly one primary DataSource candidate.
 * <p>
 * Flyway keeps using spring.flyway.* (owner) via its own DataSource,
 * not this @Primary bean. Runtime always goes through
 * RlsDataSource -> RlsConnection -> Hikari -> app_user.
 */
@Configuration
public class RlsDataSourceConfig {

  @Bean
  @ConfigurationProperties("spring.datasource.hikari")
  HikariDataSource hikariDataSource(DataSourceProperties properties) {
    HikariDataSource hikari = new HikariDataSource();
    hikari.setJdbcUrl(properties.getUrl());
    hikari.setUsername(properties.getUsername());
    hikari.setPassword(properties.getPassword());
    if (properties.getDriverClassName() != null) {
      hikari.setDriverClassName(properties.getDriverClassName());
    }
    return hikari;
  }

  @Bean
  @Primary
  DataSource dataSource(HikariDataSource hikariDataSource) {
    return new RlsDataSource(hikariDataSource);
  }
}