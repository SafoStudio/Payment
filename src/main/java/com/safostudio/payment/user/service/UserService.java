package com.safostudio.payment.user.service;

import com.safostudio.payment.user.domain.User;
import com.safostudio.payment.user.exception.UserServiceException;
import com.safostudio.payment.user.repository.UserRepository;
import com.safostudio.payment.user.service.dto.CreateUserRequest;
import com.safostudio.payment.user.service.dto.UpdateUserRequest;
import com.safostudio.payment.user.service.dto.UserResponse;
import com.safostudio.payment.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw UserServiceException.emailAlreadyExists(request.getEmail());
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw UserServiceException.phoneAlreadyExists(request.getPhone());
        }

        User user = User.create(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getRole()
        );

        if (request.getPhone() != null) {
            user.updatePhone(request.getPhone());
        }

        User saved = userRepository.save(user);

        return UserResponse.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        User user = findUserById(userId);
        return UserResponse.fromDomain(user);
    }

    @Transactional(readOnly = true)
    public User getUserEntity(UUID userId) {
        return findUserById(userId);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> UserServiceException.notFoundByEmail(email));
        return UserResponse.fromDomain(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(UserResponse::fromDomain)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getActiveUsers() {
        return userRepository.findAllActive().stream()
                .map(UserResponse::fromDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = findUserById(userId);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw UserServiceException.emailAlreadyExists(request.getEmail());
            }
            user.updateEmail(request.getEmail());
        }

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw UserServiceException.phoneAlreadyExists(request.getPhone());
            }
            user.updatePhone(request.getPhone());
        }

        if (request.getFirstName() != null || request.getLastName() != null) {
            String firstName = request.getFirstName() != null ? request.getFirstName() : user.getFirstName();
            String lastName = request.getLastName() != null ? request.getLastName() : user.getLastName();
            user.updateName(firstName, lastName);
        }

        User saved = userRepository.save(user);

        return UserResponse.fromDomain(saved);
    }

    @Transactional
    public UserResponse blockUser(UUID userId) {
        User user = findUserById(userId);

        if (user.isDeleted()) {
            throw UserServiceException.deleted(userId);
        }

        user.block();
        User saved = userRepository.save(user);

        return UserResponse.fromDomain(saved);
    }

    @Transactional
    public UserResponse activateUser(UUID userId) {
        User user = findUserById(userId);

        if (user.isDeleted()) {
            throw UserServiceException.deleted(userId);
        }

        user.activate();
        User saved = userRepository.save(user);

        return UserResponse.fromDomain(saved);
    }

    @Transactional
    public UserResponse deleteUser(UUID userId) {
        User user = findUserById(userId);

        boolean hasWallets = walletRepository.existsByOwnerId(userId.toString());
        if (hasWallets) {
            throw UserServiceException.cannotDeleteWithWallets(userId);
        }

        user.delete();
        return UserResponse.fromDomain(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public boolean isUserActive(UUID userId) {
        return userRepository.isUserActive(userId);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> UserServiceException.notFound(userId));
    }
}