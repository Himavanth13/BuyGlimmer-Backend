package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.RefundService;
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
@RequestMapping("/api/v1/refunds")
public class RefundController {

    private static final Logger logger = LoggerFactory.getLogger(RefundController.class);

    private final AuthService authService;
    private final RefundService refundService;
    private final ApiResponseFactory apiResponseFactory;

    public RefundController(AuthService authService, RefundService refundService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.refundService = refundService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/create")
    public ApiWrapperResponse<FintechDtos.RefundResponse> create(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.RefundCreateRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/refunds/create requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Refund initiated successfully", refundService.createRefund(request.data()));
    }

    @PostMapping("/detail")
    public ApiWrapperResponse<FintechDtos.RefundResponse> detail(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.RefundDetailRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/refunds/detail requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Refund details fetched successfully", refundService.getRefund(request.data()));
    }

    @PostMapping("/list")
    public ApiWrapperResponse<List<FintechDtos.RefundResponse>> list(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.RefundListRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/refunds/list requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Refunds fetched successfully", refundService.listRefunds(request.data()));
    }
}
