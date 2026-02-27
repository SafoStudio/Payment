package com.safostudio.payment.user.repository;

import com.safostudio.payment.user.domain.User;
import org.springframework.data.jdbc.repository.query.Modifying;
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

    Optional<User> findByPhone(String phone);

    List<User> findAllByStatus(User.UserStatus status);

    List<User> findAllByRole(User.UserRole role);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT * FROM users WHERE status = 'ACTIVE'")
    List<User> findAllActive();

    @Query("SELECT * FROM users WHERE email LIKE %:email% OR first_name LIKE %:name% OR last_name LIKE %:name%")
    List<User> search(@Param("email") String email, @Param("name") String name);

    @Modifying
    @Query("UPDATE users SET status = :status, updated_at = NOW() WHERE id = :userId")
    boolean updateStatus(@Param("userId") UUID userId, @Param("status") User.UserStatus status);

    @Query("SELECT COUNT(*) > 0 FROM users WHERE id = :userId AND status = 'ACTIVE'")
    boolean isUserActive(@Param("userId") UUID userId);
}