package com.buyglimmer.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            String guestId
    ) {
    }

    public record RegisterRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String phone
    ) {
    }

    public record ForgotPasswordRequest(
            @Email @NotBlank String email
    ) {
    }

    public record ForgotPasswordResponse(
            String resetToken,
            long expiresInSeconds
    ) {
    }

    public record ResetPasswordRequest(
            @Email @NotBlank String email,
            @NotBlank String resetToken,
            @NotBlank String newPassword
    ) {
    }

    public record PasswordResetResponse(
            String message
    ) {
    }

    public record AuthResponse(
            String token,
            UserDtos.UserProfileResponse user
    ) {
    }
}