package com.buyglimmer.backend.service;

import com.buyglimmer.backend.BuyGlimmerProperties;
import com.buyglimmer.backend.dto.AuthDtos;
import com.buyglimmer.backend.dto.UserDtos;
import com.buyglimmer.backend.exception.ApiException;
import com.buyglimmer.backend.repository.UserStoredProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final BuyGlimmerProperties properties;
    private final UserService userService;
    private final UserStoredProcedureRepository userRepository;
    private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();
    private final Map<String, ResetTokenInfo> passwordResetTokens = new ConcurrentHashMap<>();

    public AuthService(BuyGlimmerProperties properties, UserService userService, UserStoredProcedureRepository userRepository) {
        this.properties = properties;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserStoredProcedureRepository.StoredUser storedUser = userRepository.fetchUserByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login credentials", java.util.List.of("Use a registered BuyGlimmer account.")));

        if (storedUser.password() == null || !storedUser.password().equals(request.password())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login credentials", java.util.List.of("Use the registered BuyGlimmer email to access the demo session."));
        }

        String token = createToken();
        activeTokens.add(token);
        UserDtos.UserProfileResponse profile = userRepository.fetchProfile(storedUser.id());
        return new AuthDtos.AuthResponse(token, profile);
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        UserDtos.UserProfileResponse profile = userService.register(request.name(), request.email(), request.password(), request.phone());
        String token = createToken();
        activeTokens.add(token);
        return new AuthDtos.AuthResponse(token, profile);
    }

    public AuthDtos.ForgotPasswordResponse forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        userRepository.fetchUserByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found", java.util.List.of("Use a registered BuyGlimmer account.")));

        String resetToken = "rst-" + UUID.randomUUID();
        long expiresAtEpochSeconds = Instant.now().plusSeconds(15 * 60).getEpochSecond();
        passwordResetTokens.put(resetToken, new ResetTokenInfo(request.email(), expiresAtEpochSeconds));
        return new AuthDtos.ForgotPasswordResponse(resetToken, 15 * 60L);
    }

    public AuthDtos.PasswordResetResponse resetPassword(AuthDtos.ResetPasswordRequest request) {
        ResetTokenInfo tokenInfo = passwordResetTokens.get(request.resetToken());
        if (tokenInfo == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid reset token", java.util.List.of("Generate a new token using /api/v1/auth/forgot-password."));
        }
        if (tokenInfo.expiresAtEpochSeconds() < Instant.now().getEpochSecond()) {
            passwordResetTokens.remove(request.resetToken());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset token expired", java.util.List.of("Generate a new token using /api/v1/auth/forgot-password."));
        }
        if (!tokenInfo.email().equalsIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset token does not match the provided email");
        }

        int updatedRows = userRepository.updatePasswordByEmail(request.email(), request.newPassword());
        if (updatedRows <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Account not found", java.util.List.of("Use a registered BuyGlimmer account."));
        }

        passwordResetTokens.remove(request.resetToken());
        return new AuthDtos.PasswordResetResponse("Password reset successful");
    }

    public void requireAuthorization(String authorization) {
        String token = extractBearerToken(authorization);
        validateToken(token);
    }

    public void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing authorization token", java.util.List.of("Authenticate with /api/v1/auth/login or /api/v1/auth/register first."));
        }
        if (!activeTokens.contains(token)) {
            logger.warn("Rejected token validation for inactive token");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or missing authorization token", java.util.List.of("Authenticate with /api/v1/auth/login or /api/v1/auth/register first."));
        }
    }

    private String createToken() {
        return properties.getTokenPrefix() + "-" + UUID.randomUUID();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing Authorization header");
        }
        if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return authorization.trim();
    }

    private record ResetTokenInfo(
            String email,
            long expiresAtEpochSeconds
    ) {
    }
}