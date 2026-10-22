package com.safostudio.payment.user.repository;

import com.safostudio.payment.user.domain.Role;
import com.safostudio.payment.shared.security.AuthorityRow;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends CrudRepository<Role, UUID> {

  Optional<Role> findByCode(String code);

  List<Role> findAllByCodeIn(List<String> codes);

  @Query("""
            SELECT r.code FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = :userId
            """)
  List<String> findRoleCodesByUserId(@Param("userId") UUID userId);

  @Query("""
            SELECT 'ROLE' AS authority_type, r.code AS code
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = :userId
            UNION ALL
            SELECT 'PERMISSION' AS authority_type, p.code AS code
            FROM permissions p
            JOIN role_permissions rp ON rp.permission_id = p.id
            JOIN user_roles ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = :userId
            """)
  List<AuthorityRow> findAuthoritiesByUserId(@Param("userId") UUID userId);
}