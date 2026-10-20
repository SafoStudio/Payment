package com.safostudio.payment.shared.rls;

import com.safostudio.payment.shared.security.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * DataSource wrapper that injects the current user (id + role codes) into
 * app.current_user_id / app.role on the connection before the first statement.
 * <p>
 * Source of the context: SecurityContextHolder -> Authentication -> AppUserDetails.
 * AppUserDetails carries userId, so no DB lookup is needed here
 * (which would create a UserDetailsService -> DataSource cycle).
 * <p>
 * Anonymous / unauthenticated requests get empty context; RLS policies fail closed
 * (no rows). This is intentional: nothing should read data without an identity.
 */
public class RlsDataSource implements DataSource {

  private final DataSource delegate;

  public RlsDataSource(DataSource delegate) {
    this.delegate = delegate;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return wrap(delegate.getConnection());
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return wrap(delegate.getConnection(username, password));
  }

  private Connection wrap(Connection connection) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    UUID userId = null;
    String rolesCsv = "";

    if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AppUserDetails appUser) {
      userId = appUser.getUserId();
      rolesCsv = appUser.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
              .filter(a -> a.startsWith("ROLE_"))
              .map(a -> a.substring("ROLE_".length()))
              .collect(Collectors.joining(","));
    }

    return new RlsConnection(connection, userId, rolesCsv);
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