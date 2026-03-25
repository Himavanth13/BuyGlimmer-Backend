package com.buyglimmer.backend.service;

import com.buyglimmer.backend.BuyGlimmerProperties;
import com.buyglimmer.backend.dto.AuthDtos;
import com.buyglimmer.backend.dto.UserDtos;
import com.buyglimmer.backend.exception.ApiException;
import com.buyglimmer.backend.repository.CartProcedureRepository;
import com.buyglimmer.backend.repository.PasswordResetTokenRepository;
import com.buyglimmer.backend.repository.UserStoredProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final BuyGlimmerProperties properties;
    private final UserService userService;
    private final UserStoredProcedureRepository userRepository;
    private final CartProcedureRepository cartProcedureRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetEmailService passwordResetEmailService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, String> tokenOwners = new ConcurrentHashMap<>();

    public AuthService(
            BuyGlimmerProperties properties,
            UserService userService,
            UserStoredProcedureRepository userRepository,
            CartProcedureRepository cartProcedureRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetEmailService passwordResetEmailService) {
        this.properties = properties;
        this.userService = userService;
        this.userRepository = userRepository;
        this.cartProcedureRepository = cartProcedureRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetEmailService = passwordResetEmailService;
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserStoredProcedureRepository.StoredUser storedUser = userRepository.fetchUserByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login credentials", java.util.List.of("Use a registered BuyGlimmer account.")));

        if (storedUser.password() == null || !passwordEncoder.matches(request.password(), storedUser.password())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login credentials", java.util.List.of("Use the registered BuyGlimmer email to access the demo session."));
        }

        if (request.guestId() != null && !request.guestId().isBlank()) {
            int mergedItems = cartProcedureRepository.mergeGuestCartIntoCustomer(request.guestId(), storedUser.id());
            logger.info("Merged {} guest cart items guestId={} into customerId={}", mergedItems, request.guestId(), storedUser.id());
        }

        String token = createToken();
        tokenOwners.put(token, storedUser.id());
        UserDtos.UserProfileResponse profile = userRepository.fetchProfile(storedUser.id());
        return new AuthDtos.AuthResponse(token, profile);
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());
        UserDtos.UserProfileResponse profile = userService.register(request.name(), request.email(), passwordHash, request.phone());
        String token = createToken();
        tokenOwners.put(token, profile.id());
        return new AuthDtos.AuthResponse(token, profile);
    }

    public AuthDtos.ForgotPasswordResponse forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        // Verify account exists
        UserStoredProcedureRepository.StoredUser storedUser = userRepository.fetchUserByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found", java.util.List.of("Use a registered BuyGlimmer account.")));

        // Generate reset token
        String resetToken = "rst-" + UUID.randomUUID();
        long expiresAtEpochSeconds = Instant.now().plusSeconds(15 * 60).getEpochSecond();

        // Save token to database
        passwordResetTokenRepository.createPasswordResetToken(request.email(), resetToken, expiresAtEpochSeconds);

        // Send email with reset token
        passwordResetEmailService.sendPasswordResetToken(request.email(), resetToken, 15 * 60L);

        logger.info("Forgot password request processed for email={}", request.email());
        return new AuthDtos.ForgotPasswordResponse(resetToken, 15 * 60L);
    }

    public AuthDtos.PasswordResetResponse resetPassword(AuthDtos.ResetPasswordRequest request) {
        // Retrieve token from database
        PasswordResetTokenRepository.ResetTokenInfo tokenInfo = passwordResetTokenRepository.getPasswordResetToken(request.resetToken())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid reset token", java.util.List.of("Generate a new token using /api/v1/auth/forgot-password.")));

        // Check if token is already used
        if (tokenInfo.isUsed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset token already used", java.util.List.of("Generate a new token using /api/v1/auth/forgot-password."));
        }

        // Check if token is expired
        if (tokenInfo.expiresAtEpochSeconds() < Instant.now().getEpochSecond()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset token expired", java.util.List.of("Generate a new token using /api/v1/auth/forgot-password."));
        }

        // Verify email matches
        if (!tokenInfo.email().equalsIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset token does not match the provided email");
        }

        // Update password in database
        String passwordHash = passwordEncoder.encode(request.newPassword());
        int updatedRows = userRepository.updatePasswordByEmail(request.email(), passwordHash);
        if (updatedRows <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Account not found", java.util.List.of("Use a registered BuyGlimmer account."));
        }

        // Mark token as used
        passwordResetTokenRepository.markPasswordResetTokenUsed(request.resetToken());

        // Send success email
        UserStoredProcedureRepository.StoredUser storedUser = userRepository.fetchUserByEmail(request.email()).orElse(null);
        if (storedUser != null) {
            passwordResetEmailService.sendPasswordResetSuccessEmail(request.email(), storedUser.name());
        }

        logger.info("Password reset completed for email={}", request.email());
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
        if (!tokenOwners.containsKey(token)) {
            logger.warn("Rejected token validation for inactive token");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or missing authorization token", java.util.List.of("Authenticate with /api/v1/auth/login or /api/v1/auth/register first."));
        }
    }

    public String getAuthenticatedCustomerId(String token) {
        validateToken(token);
        String customerId = tokenOwners.get(token);
        if (customerId == null || customerId.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or missing authorization token", java.util.List.of("Authenticate with /api/v1/auth/login or /api/v1/auth/register first."));
        }
        return customerId;
    }

    public void assertCustomerOwnership(String token, String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "customerId is required");
        }
        String authenticatedCustomerId = getAuthenticatedCustomerId(token);
        if (!authenticatedCustomerId.equals(customerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied for requested customerId");
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
}