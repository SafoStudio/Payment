package com.safostudio.payment.user.dto;

import com.safostudio.payment.user.domain.User;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class UserResponse {
  UUID id;
  String email;
  String phone;
  String firstName;
  String lastName;
  String fullName;
  User.UserRole role;
  User.UserStatus status;
  Instant createdAt;
  Instant updatedAt;

  public static UserResponse fromDomain(User user) {
    return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .phone(user.getPhone())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFirstName() + " " + user.getLastName())
            .role(user.getRole())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
  }
}