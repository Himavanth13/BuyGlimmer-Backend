package com.buyglimmer.backend.service;

import com.buyglimmer.backend.BuyGlimmerProperties;
import com.buyglimmer.backend.dto.AuthDtos;
import com.buyglimmer.backend.dto.UserDtos;
import com.buyglimmer.backend.exception.ApiException;
import com.buyglimmer.backend.repository.UserStoredProcedureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final BuyGlimmerProperties properties;
    private final UserService userService;
    private final UserStoredProcedureRepository userRepository;
    private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();

    public AuthService(BuyGlimmerProperties properties, UserService userService, UserStoredProcedureRepository userRepository) {
        this.properties = properties;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserStoredProcedureRepository.StoredUser storedUser = userRepository.fetchUserByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login credentials", java.util.List.of("Use a registered BuyGlimmer account.")));

        if (!storedUser.password().equals(request.password())) {
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

    public void requireAuthorization(String authorization) {
        String token = extractBearerToken(authorization);
        if (!activeTokens.contains(token)) {
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
}