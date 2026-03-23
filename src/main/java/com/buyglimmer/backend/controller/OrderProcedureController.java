package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.OrderProcedureService;
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
@RequestMapping("/api/v1/orders")
public class OrderProcedureController {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcedureController.class);

    private final AuthService authService;
    private final OrderProcedureService orderProcedureService;
    private final ApiResponseFactory apiResponseFactory;

    public OrderProcedureController(AuthService authService, OrderProcedureService orderProcedureService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.orderProcedureService = orderProcedureService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/create")
    public ApiWrapperResponse<FintechDtos.OrderSummaryResponse> createOrder(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.OrderCreateRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/orders/create requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Order created successfully", orderProcedureService.createOrder(request.data()));
    }

    @PostMapping("/instant-buy")
    public ApiWrapperResponse<FintechDtos.OrderSummaryResponse> instantBuy(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.InstantBuyRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/orders/instant-buy requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Instant buy order created successfully", orderProcedureService.instantBuy(request.data()));
    }

    @PostMapping("/list")
    public ApiWrapperResponse<List<FintechDtos.OrderSummaryResponse>> getOrders(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.OrderListRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/orders/list requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Orders fetched successfully", orderProcedureService.getOrders(request.data()));
    }

    @PostMapping("/detail")
    public ApiWrapperResponse<FintechDtos.OrderDetailResponse> getOrderDetail(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.OrderDetailRequest> request) {
        String authenticatedCustomerId = authService.getAuthenticatedCustomerId(request.token());
        logger.info("POST /api/v1/orders/detail requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Order detail fetched successfully", orderProcedureService.getOrderDetailForCustomer(authenticatedCustomerId, request.data()));
    }
}
