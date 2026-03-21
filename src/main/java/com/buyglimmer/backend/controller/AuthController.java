package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.AuthDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.util.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final ApiResponseFactory apiResponseFactory;

    public AuthController(AuthService authService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/register")
    public ApiWrapperResponse<AuthDtos.AuthResponse> register(@Valid @RequestBody ApiWrapperRequest<AuthDtos.RegisterRequest> request) {
        return apiResponseFactory.success(request.requestId(), "Registration successful", authService.register(request.data()));
    }

    @PostMapping("/login")
    public ApiWrapperResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody ApiWrapperRequest<AuthDtos.LoginRequest> request) {
        return apiResponseFactory.success(request.requestId(), "Login successful", authService.login(request.data()));
    }

    @PostMapping("/forgot-password")
    public ApiWrapperResponse<AuthDtos.ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ApiWrapperRequest<AuthDtos.ForgotPasswordRequest> request) {
        return apiResponseFactory.success(request.requestId(), "Password reset token generated", authService.forgotPassword(request.data()));
    }

    @PostMapping("/reset-password")
    public ApiWrapperResponse<AuthDtos.PasswordResetResponse> resetPassword(
            @Valid @RequestBody ApiWrapperRequest<AuthDtos.ResetPasswordRequest> request) {
        return apiResponseFactory.success(request.requestId(), "Password reset completed", authService.resetPassword(request.data()));
    }
}