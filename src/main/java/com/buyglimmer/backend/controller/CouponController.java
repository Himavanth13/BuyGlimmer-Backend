package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.CouponProcedureService;
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
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private static final Logger logger = LoggerFactory.getLogger(CouponController.class);

    private final AuthService authService;
    private final CouponProcedureService couponProcedureService;
    private final ApiResponseFactory apiResponseFactory;

    public CouponController(AuthService authService, CouponProcedureService couponProcedureService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.couponProcedureService = couponProcedureService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/validate")
    public ApiWrapperResponse<FintechDtos.CouponValidationResponse> validateCoupon(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.CouponValidateRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/coupons/validate requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Coupon validation completed", couponProcedureService.validateCoupon(request.data()));
    }

    @PostMapping("/list")
    public ApiWrapperResponse<List<FintechDtos.CouponSummaryResponse>> listCoupons(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.CouponListRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/coupons/list requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Coupons fetched successfully", couponProcedureService.listCoupons(request.data()));
    }
}
