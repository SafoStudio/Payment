package com.safostudio.payment.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    private UUID id;

    private String email;

    private String phone;

    private String firstName;

    private String lastName;

    private UserRole role;

    private UserStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    public enum UserRole {
        USER,
        ADMIN,
        MERCHANT,
        SUPPORT
    }

    public enum UserStatus {
        ACTIVE,
        BLOCKED,
        DELETED
    }

    public static User create(String email, String firstName, String lastName, UserRole role) {
        return User.builder()
                .id(null)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role != null ? role : UserRole.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void block() {
        if (this.status == UserStatus.ACTIVE) {
            this.status = UserStatus.BLOCKED;
            this.updatedAt = Instant.now();
        }
    }

    public void activate() {
        if (this.status == UserStatus.BLOCKED) {
            this.status = UserStatus.ACTIVE;
            this.updatedAt = Instant.now();
        }
    }

    public void delete() {
        this.status = UserStatus.DELETED;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return status == UserStatus.BLOCKED;
    }

    public boolean isDeleted() {
        return status == UserStatus.DELETED;
    }

    public void updateEmail(String email) {
        this.email = email;
        this.updatedAt = Instant.now();
    }

    public void updatePhone(String phone) {
        this.phone = phone;
        this.updatedAt = Instant.now();
    }

    public void updateName(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.updatedAt = Instant.now();
    }
}