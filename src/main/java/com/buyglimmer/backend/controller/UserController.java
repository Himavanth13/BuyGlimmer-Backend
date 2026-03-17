package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiResponse;
import com.buyglimmer.backend.dto.UserDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/profile")
    public ApiResponse<UserDtos.UserProfileResponse> profile(@RequestHeader("Authorization") String authorization) {
        authService.requireAuthorization(authorization);
        return new ApiResponse<>("user-profile", "success", userService.fetchProfile());
    }

    @PostMapping("/profile/update")
    public ApiResponse<UserDtos.UserProfileResponse> updateProfile(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UserDtos.UpdateProfileRequest request) {
        authService.requireAuthorization(authorization);
        return new ApiResponse<>("user-profile-update", "success", userService.updateProfile(request));
    }
}