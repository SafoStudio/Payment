package com.safostudio.payment.shared.jdbc;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Enum -> String for VARCHAR-backed enum columns.
 * <p>
 * Registered in userConverters() to take precedence over the generic
 * String -> PGobject converter, which otherwise makes Spring Data JDBC
 * try Enum -> String -> PGobject for plain VARCHAR columns and fail
 * with ConverterNotFoundException.
 */
@WritingConverter
public class EnumToStringConverter implements Converter<Enum<?>, String> {

  @Override
  public String convert(Enum<?> source) {
    return source.name();
  }
}