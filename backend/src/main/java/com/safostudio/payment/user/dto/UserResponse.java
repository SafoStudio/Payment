package com.safostudio.payment.user.dto;

import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.user.domain.UserStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
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
  List<String> roles;
  UserStatus status;
  Instant createdAt;
  Instant updatedAt;

  public static UserResponse fromDomain(User user, List<String> roleCodes) {
    return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .phone(user.getPhone())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFirstName() + " " + user.getLastName())
            .roles(roleCodes)
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
  }
}