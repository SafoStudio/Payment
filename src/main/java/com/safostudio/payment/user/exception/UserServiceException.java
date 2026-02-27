package com.safostudio.payment.user.exception;

import java.util.UUID;

public class UserServiceException extends RuntimeException {

    private UserServiceException(String message) {
        super(message);
    }

    public static UserServiceException notFound(UUID userId) {
        return new UserServiceException("User not found: %s".formatted(userId));
    }

    public static UserServiceException notFoundByEmail(String email) {
        return new UserServiceException("User not found with email: %s".formatted(email));
    }

    public static UserServiceException emailAlreadyExists(String email) {
        return new UserServiceException("User with email %s already exists".formatted(email));
    }

    public static UserServiceException phoneAlreadyExists(String phone) {
        return new UserServiceException("User with phone %s already exists".formatted(phone));
    }

    public static UserServiceException invalidEmail(String email) {
        return new UserServiceException("Invalid email format: %s".formatted(email));
    }

    public static UserServiceException notActive(UUID userId) {
        return new UserServiceException("User %s is not active".formatted(userId));
    }

    public static UserServiceException blocked(UUID userId) {
        return new UserServiceException("User %s is blocked".formatted(userId));
    }

    public static UserServiceException deleted(UUID userId) {
        return new UserServiceException("User %s is deleted".formatted(userId));
    }

    public static UserServiceException cannotDeleteWithWallets(UUID userId) {
        return new UserServiceException("Cannot delete user %s with active wallets".formatted(userId));
    }
}