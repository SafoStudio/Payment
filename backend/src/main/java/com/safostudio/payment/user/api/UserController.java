package com.safostudio.payment.user.api;

import com.safostudio.payment.user.api.input.CreateUserInput;
import com.safostudio.payment.user.api.input.UpdateUserInput;
import com.safostudio.payment.user.service.UserService;
import com.safostudio.payment.user.dto.CreateUserRequest;
import com.safostudio.payment.user.dto.UpdateUserRequest;
import com.safostudio.payment.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PreAuthorize("hasAuthority('user:read:all')")
  @QueryMapping
  public UserResponse user(@Argument UUID id) {
    return userService.getUser(id);
  }

  @PreAuthorize("hasAuthority('user:read:all')")
  @QueryMapping
  public UserResponse userByEmail(@Argument String email) {
    return userService.getUserByEmail(email);
  }

  @PreAuthorize("hasAuthority('user:read:all')")
  @QueryMapping
  public List<UserResponse> users() {
    return userService.getAllUsers();
  }

  @PreAuthorize("hasAuthority('user:read:all')")
  @QueryMapping
  public List<UserResponse> activeUsers() {
    return userService.getActiveUsers();
  }

  @PreAuthorize("hasAuthority('user:create')")
  @MutationMapping
  public UserResponse createUser(@Argument CreateUserInput input) {
    CreateUserRequest request = CreateUserRequest.builder()
            .email(input.email())
            .phone(input.phone())
            .firstName(input.firstName())
            .lastName(input.lastName())
            .roleCodes(input.roleCodes())
            .build();
    return userService.createUser(request);
  }

  @PreAuthorize("hasAuthority('user:update')")
  @MutationMapping
  public UserResponse updateUser(@Argument UUID id, @Argument UpdateUserInput input) {
    UpdateUserRequest request = UpdateUserRequest.builder()
            .email(input.email())
            .phone(input.phone())
            .firstName(input.firstName())
            .lastName(input.lastName())
            .build();
    return userService.updateUser(id, request);
  }

  @PreAuthorize("hasAuthority('user:block')")
  @MutationMapping
  public UserResponse blockUser(@Argument UUID id) {
    return userService.blockUser(id);
  }

  @PreAuthorize("hasAuthority('user:block')")
  @MutationMapping
  public UserResponse activateUser(@Argument UUID id) {
    return userService.activateUser(id);
  }

  @PreAuthorize("hasAuthority('user:block')")
  @MutationMapping
  public UserResponse deleteUser(@Argument UUID id) {
    return userService.deleteUser(id);
  }

  @PreAuthorize("hasAuthority('user:assign_role')")
  @MutationMapping
  public UserResponse assignRoles(@Argument UUID userId, @Argument List<String> roleCodes) {
    return userService.assignRoles(userId, roleCodes);
  }

  @PreAuthorize("hasAuthority('user:assign_role')")
  @MutationMapping
  public UserResponse addRole(@Argument UUID userId, @Argument String roleCode) {
    return userService.addRole(userId, roleCode);
  }

  @PreAuthorize("hasAuthority('user:assign_role')")
  @MutationMapping
  public UserResponse removeRole(@Argument UUID userId, @Argument String roleCode) {
    return userService.removeRole(userId, roleCode);
  }

  @PreAuthorize("hasAuthority('user:read:all')")
  @QueryMapping
  public List<UserResponse> usersByRole(@Argument String roleCode) {
    return userService.getUsersByRoleCode(roleCode);
  }
}