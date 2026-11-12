package com.safostudio.payment.shared.jdbc;

import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.util.List;

@Configuration
public class JdbcConfig extends AbstractJdbcConfiguration {

  @NullMarked
  @Override
  protected List<?> userConverters() {
    return List.of(
            new EnumToStringConverter()
    );
  }
}