package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.DeliveryService;
import com.buyglimmer.backend.service.OrderProcedureService;
import com.buyglimmer.backend.util.ApiResponseFactory;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery")
public class DeliveryController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);

    private final AuthService authService;
    private final DeliveryService deliveryService;
    private final OrderProcedureService orderProcedureService;
    private final ApiResponseFactory apiResponseFactory;

    public DeliveryController(
            AuthService authService,
            DeliveryService deliveryService,
            OrderProcedureService orderProcedureService,
            ApiResponseFactory apiResponseFactory
    ) {
        this.authService = authService;
        this.deliveryService = deliveryService;
        this.orderProcedureService = orderProcedureService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/create")
    public ApiWrapperResponse<FintechDtos.DeliveryResponse> create(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.DeliveryCreateRequest> request) {
        String authenticatedCustomerId = authService.getAuthenticatedCustomerId(request.token());
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        orderProcedureService.getOrderDetailForCustomer(
            authenticatedCustomerId,
            new FintechDtos.OrderDetailRequest(request.data().orderId())
        );
        logger.info("POST /api/v1/delivery/create requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Delivery created successfully", deliveryService.createDelivery(request.data()));
    }

    @PostMapping("/detail")
    public ApiWrapperResponse<FintechDtos.DeliveryResponse> detail(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.DeliveryDetailRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/delivery/detail requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Delivery details fetched successfully", deliveryService.getDelivery(request.data()));
    }

    @PostMapping("/update-status")
    public ApiWrapperResponse<FintechDtos.DeliveryResponse> updateStatus(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.DeliveryStatusUpdateRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/delivery/update-status requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Delivery status updated successfully", deliveryService.updateStatus(request.data()));
    }
}
