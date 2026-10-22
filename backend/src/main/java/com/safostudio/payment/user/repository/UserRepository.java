package com.safostudio.payment.user.repository;

import com.safostudio.payment.user.domain.User;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends CrudRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByPhone(String phone);

  @Query("SELECT * FROM users WHERE status = 'ACTIVE'")
  List<User> findAllActive();

  @Query("""
          SELECT u.* FROM users u
          JOIN user_roles ur ON ur.user_id = u.id
          JOIN roles r ON r.id = ur.role_id
          WHERE r.code = :roleCode
          """)
  List<User> findAllByRoleCode(@Param("roleCode") String roleCode);
}