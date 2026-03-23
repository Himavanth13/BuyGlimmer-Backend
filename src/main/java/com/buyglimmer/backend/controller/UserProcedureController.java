package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.UserProcedureService;
import com.buyglimmer.backend.util.ApiResponseFactory;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserProcedureController {

    private static final Logger logger = LoggerFactory.getLogger(UserProcedureController.class);

    private final AuthService authService;
    private final UserProcedureService userProcedureService;
    private final ApiResponseFactory apiResponseFactory;

    public UserProcedureController(AuthService authService, UserProcedureService userProcedureService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.userProcedureService = userProcedureService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/profile")
    public ApiWrapperResponse<FintechDtos.UserProfileResponse> profile(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.UserProfileRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/user/profile requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Profile fetched successfully", userProcedureService.getProfile(request.data()));
    }

    @PostMapping("/update")
    public ApiWrapperResponse<FintechDtos.UserProfileResponse> update(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.UserUpdateRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/user/update requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Profile updated successfully", userProcedureService.updateProfile(request.data()));
    }
}
