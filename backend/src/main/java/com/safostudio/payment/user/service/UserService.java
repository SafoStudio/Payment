package com.safostudio.payment.user.service;

import com.safostudio.payment.shared.exception.BusinessException;
import com.safostudio.payment.shared.security.AppUserDetails;
import com.safostudio.payment.user.domain.Role;
import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.user.dto.CreateUserRequest;
import com.safostudio.payment.user.dto.UpdateUserRequest;
import com.safostudio.payment.user.dto.UserResponse;
import com.safostudio.payment.user.repository.RoleRepository;
import com.safostudio.payment.user.repository.UserRepository;
import com.safostudio.payment.user.repository.UserRoleRepository;
import com.safostudio.payment.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final WalletRepository walletRepository;
  private final PublicKeyCredentialUserEntityRepository userEntities;
  private final UserCredentialRepository userCredentials;

  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw BusinessException.userAlreadyExists("email", request.getEmail());
    }

    if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
      throw BusinessException.userAlreadyExists("phone", request.getPhone());
    }

    User user = User.create(request.getEmail(), request.getFirstName(), request.getLastName());

    if (request.getPhone() != null) {
      user.updatePhone(request.getPhone());
    }

    User saved = userRepository.save(user);

    List<Role> roles = resolveRoles(request.getRoleCodes());
    userRoleRepository.replaceRoles(saved.getId(), roles.stream().map(Role::getId).toList());

    return UserResponse.fromDomain(saved, roles.stream().map(Role::getCode).toList(), List.of(), null);
  }

  @Transactional(readOnly = true)
  public UserResponse getUser(UUID userId) {
    User user = findUserById(userId);
    return toResponse(user);
  }

  @Transactional(readOnly = true)
  public UserResponse getUserByEmail(String email) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> BusinessException.userNotFoundByEmail(email));
    return toResponse(user);
  }

  @Transactional(readOnly = true)
  public UserResponse getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
      throw new IllegalStateException("getCurrentUser called without authenticated AppUserDetails principal");
    }

    User user = findUserById(principal.getUserId());

    List<String> roleCodes = new ArrayList<>();
    List<String> permissions = new ArrayList<>();
    for (GrantedAuthority authority : principal.getAuthorities()) {
      String code = authority.getAuthority();
      if (code == null) {
        continue;
      }
      if (code.startsWith("ROLE_")) {
        roleCodes.add(code.substring("ROLE_".length()));
      } else {
        permissions.add(code);
      }
    }

    boolean hasPasskey = hasPasskey(user.getEmail());

    return UserResponse.fromDomain(user, roleCodes, permissions, hasPasskey);
  }

  @Transactional(readOnly = true)
  public List<UserResponse> getAllUsers() {
    return StreamSupport.stream(userRepository.findAll().spliterator(), false)
            .map(this::toResponse)
            .toList();
  }

  @Transactional(readOnly = true)
  public List<UserResponse> getActiveUsers() {
    return userRepository.findAllActive().stream()
            .map(this::toResponse)
            .toList();
  }

  @Transactional(readOnly = true)
  public List<UserResponse> getUsersByRoleCode(String roleCode) {
    return userRepository.findAllByRoleCode(roleCode).stream()
            .map(this::toResponse)
            .toList();
  }

  @Transactional
  public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
    User user = findUserById(userId);

    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw BusinessException.userAlreadyExists("email", request.getEmail());
      }
      user.updateEmail(request.getEmail());
    }

    if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
      if (userRepository.existsByPhone(request.getPhone())) {
        throw BusinessException.userAlreadyExists("phone", request.getPhone());
      }
      user.updatePhone(request.getPhone());
    }

    if (request.getFirstName() != null || request.getLastName() != null) {
      String firstName = request.getFirstName() != null ? request.getFirstName() : user.getFirstName();
      String lastName = request.getLastName() != null ? request.getLastName() : user.getLastName();
      user.updateName(firstName, lastName);
    }

    User saved = userRepository.save(user);

    return toResponse(saved);
  }

  @Transactional
  public UserResponse blockUser(UUID userId) {
    User user = findUserById(userId);

    if (user.isDeleted()) {
      throw BusinessException.userDeleted(userId);
    }

    user.block();
    User saved = userRepository.save(user);

    return toResponse(saved);
  }

  @Transactional
  public UserResponse activateUser(UUID userId) {
    User user = findUserById(userId);

    if (user.isDeleted()) {
      throw BusinessException.userDeleted(userId);
    }

    user.activate();
    User saved = userRepository.save(user);

    return toResponse(saved);
  }

  @Transactional
  public UserResponse deleteUser(UUID userId) {
    User user = findUserById(userId);

    boolean hasWallets = walletRepository.existsByUserId(userId);
    if (hasWallets) {
      throw BusinessException.userCannotDeleteWithWallets(userId);
    }

    user.delete();
    return toResponse(userRepository.save(user));
  }

  @Transactional
  public UserResponse assignRoles(UUID userId, List<String> roleCodes) {
    User user = findUserById(userId);

    if (user.isDeleted()) {
      throw BusinessException.userDeleted(userId);
    }

    List<Role> roles = resolveRoles(roleCodes);
    userRoleRepository.replaceRoles(userId, roles.stream().map(Role::getId).toList());

    return UserResponse.fromDomain(user, roles.stream().map(Role::getCode).toList(), List.of(), null);
  }

  @Transactional
  public UserResponse addRole(UUID userId, String roleCode) {
    User user = findUserById(userId);

    if (user.isDeleted()) {
      throw BusinessException.userDeleted(userId);
    }

    Role role = roleRepository.findByCode(roleCode)
            .orElseThrow(() -> BusinessException.invalidRoleCodes("Unknown role: " + roleCode));

    userRoleRepository.assignRole(userId, role.getId());

    return toResponse(user);
  }

  @Transactional
  public UserResponse removeRole(UUID userId, String roleCode) {
    User user = findUserById(userId);

    if (user.isDeleted()) {
      throw BusinessException.userDeleted(userId);
    }

    Role role = roleRepository.findByCode(roleCode)
            .orElseThrow(() -> BusinessException.invalidRoleCodes("Unknown role: " + roleCode));

    userRoleRepository.removeRole(userId, role.getId());

    return toResponse(user);
  }

  private boolean hasPasskey(String email) {
    PublicKeyCredentialUserEntity userEntity = userEntities.findByUsername(email);
    if (userEntity == null) {
      return false;
    }
    return !userCredentials.findByUserId(userEntity.getId()).isEmpty();
  }

  private UserResponse toResponse(User user) {
    List<String> roleCodes = roleRepository.findRoleCodesByUserId(user.getId());
    return UserResponse.fromDomain(user, roleCodes, List.of(), null);
  }

  private List<Role> resolveRoles(List<String> roleCodes) {
    if (roleCodes == null || roleCodes.isEmpty()) {
      throw BusinessException.invalidRoleCodes("At least one role is required");
    }

    List<Role> roles = roleRepository.findAllByCodeIn(roleCodes);

    if (roles.size() != roleCodes.size()) {
      List<String> found = roles.stream().map(Role::getCode).toList();
      List<String> missing = roleCodes.stream().filter(c -> !found.contains(c)).toList();
      throw BusinessException.invalidRoleCodes("Unknown role codes: " + missing);
    }

    return roles;
  }

  private User findUserById(UUID userId) {
    return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.userNotFound(userId));
  }
}