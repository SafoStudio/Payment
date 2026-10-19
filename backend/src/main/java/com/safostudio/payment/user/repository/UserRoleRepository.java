package com.safostudio.payment.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRoleRepository {

  private final JdbcTemplate jdbc;

  public void assignRole(UUID userId, UUID roleId) {
    jdbc.update("""
                INSERT INTO user_roles (user_id, role_id)
                VALUES (?, ?)
                ON CONFLICT (user_id, role_id) DO NOTHING
                """, userId, roleId);
  }

  public void removeRole(UUID userId, UUID roleId) {
    jdbc.update("DELETE FROM user_roles WHERE user_id = ? AND role_id = ?", userId, roleId);
  }

  public void replaceRoles(UUID userId, java.util.List<UUID> roleIds) {
    jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
    for (UUID roleId : roleIds) {
      assignRole(userId, roleId);
    }
  }
}