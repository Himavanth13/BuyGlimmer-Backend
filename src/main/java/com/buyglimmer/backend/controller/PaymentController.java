package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.PaymentProcedureService;
import com.buyglimmer.backend.util.ApiResponseFactory;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final AuthService authService;
    private final PaymentProcedureService paymentProcedureService;
    private final ApiResponseFactory apiResponseFactory;

    public PaymentController(AuthService authService, PaymentProcedureService paymentProcedureService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.paymentProcedureService = paymentProcedureService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/create")
    public ApiWrapperResponse<FintechDtos.PaymentResponse> createPayment(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.PaymentCreateRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/payments/create requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Payment created successfully", paymentProcedureService.createPayment(request.data()));
    }

    @PostMapping("/verify")
    public ApiWrapperResponse<FintechDtos.PaymentResponse> verifyPayment(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.PaymentVerifyRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/payments/verify requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Payment verified successfully", paymentProcedureService.verifyPayment(request.data()));
    }
}
