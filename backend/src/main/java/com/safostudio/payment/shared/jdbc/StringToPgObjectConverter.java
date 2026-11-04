package com.safostudio.payment.shared.jdbc;

import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.sql.SQLException;

@WritingConverter
public class StringToPgObjectConverter implements Converter<String, PGobject> {

  @Override
  public PGobject convert(String source) {
    try {
      PGobject object = new PGobject();
      object.setType("jsonb");
      object.setValue(source);
      return object;
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to convert String to PGobject", ex);
    }
  }
}