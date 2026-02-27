package com.safostudio.payment.user.controller;

import com.safostudio.payment.user.controller.input.CreateUserInput;
import com.safostudio.payment.user.controller.input.UpdateUserInput;
import com.safostudio.payment.user.service.UserService;
import com.safostudio.payment.user.service.dto.CreateUserRequest;
import com.safostudio.payment.user.service.dto.UpdateUserRequest;
import com.safostudio.payment.user.service.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @QueryMapping
    public UserResponse user(@Argument UUID id) {
        return userService.getUser(id);
    }

    @QueryMapping
    public UserResponse userByEmail(@Argument String email) {
        return userService.getUserByEmail(email);
    }

    @QueryMapping
    public List<UserResponse> users() {
        return userService.getAllUsers();
    }

    @QueryMapping
    public List<UserResponse> activeUsers() {
        return userService.getActiveUsers();
    }

    @MutationMapping
    public UserResponse createUser(@Argument CreateUserInput input) {
        CreateUserRequest request = CreateUserRequest.builder()
                .email(input.email())
                .phone(input.phone())
                .firstName(input.firstName())
                .lastName(input.lastName())
                .role(input.role())
                .build();
        return userService.createUser(request);
    }

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

    @MutationMapping
    public UserResponse blockUser(@Argument UUID id) {
        return userService.blockUser(id);
    }

    @MutationMapping
    public UserResponse activateUser(@Argument UUID id) {
        return userService.activateUser(id);
    }

    @MutationMapping
    public UserResponse deleteUser(@Argument UUID id) {
        return userService.deleteUser(id);
    }
}