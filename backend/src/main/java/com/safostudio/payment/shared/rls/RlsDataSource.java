package com.safostudio.payment.shared.rls;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * DataSource wrapper that returns an RlsConnection for every borrow.
 * <p>
 * The RLS context (app.current_user_id, app.role) is resolved lazily by
 * RlsConnection from SecurityContextHolder on the first statement, so a
 * SecurityContext established after the connection is borrowed is still
 * picked up. See RlsConnection for details.
 * <p>
 * Anonymous / unauthenticated requests get an empty context; RLS policies
 * fail closed (no rows). This is intentional: nothing should read data
 * without an identity.
 */
public class RlsDataSource implements DataSource {

  private final DataSource delegate;

  public RlsDataSource(DataSource delegate) {
    this.delegate = delegate;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return new RlsConnection(delegate.getConnection());
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return new RlsConnection(delegate.getConnection(username, password));
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException { return delegate.getLogWriter(); }
  @Override
  public void setLogWriter(PrintWriter out) throws SQLException { delegate.setLogWriter(out); }
  @Override
  public void setLoginTimeout(int seconds) throws SQLException { delegate.setLoginTimeout(seconds); }
  @Override
  public int getLoginTimeout() throws SQLException { return delegate.getLoginTimeout(); }
  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException { return delegate.getParentLogger(); }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return iface.cast(this);
    }
    return delegate.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return iface.isInstance(this) || delegate.isWrapperFor(iface);
  }
}