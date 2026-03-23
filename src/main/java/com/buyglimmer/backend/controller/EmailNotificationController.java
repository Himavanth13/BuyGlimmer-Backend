package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.EmailNotificationService;
import com.buyglimmer.backend.util.ApiResponseFactory;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/email")
public class EmailNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationController.class);

    private final AuthService authService;
    private final EmailNotificationService emailNotificationService;
    private final ApiResponseFactory apiResponseFactory;

    public EmailNotificationController(AuthService authService, EmailNotificationService emailNotificationService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.emailNotificationService = emailNotificationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/send")
    public ApiWrapperResponse<FintechDtos.EmailNotificationResponse> send(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.EmailNotificationSendRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/notifications/email/send requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Email notification sent successfully", emailNotificationService.send(request.data()));
    }

    @PostMapping("/history")
    public ApiWrapperResponse<List<FintechDtos.EmailNotificationResponse>> history(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.EmailNotificationHistoryRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/notifications/email/history requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Email notification history fetched successfully", emailNotificationService.history(request.data()));
    }
}
