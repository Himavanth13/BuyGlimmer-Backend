package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiResponse;
import com.buyglimmer.backend.dto.OrderDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final AuthService authService;
    private final OrderService orderService;

    public OrderController(AuthService authService, OrderService orderService) {
        this.authService = authService;
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ApiResponse<OrderDtos.OrderResponse> checkout(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody OrderDtos.CheckoutRequest request) {
        authService.requireAuthorization(authorization);
        return new ApiResponse<>("checkout", "success", orderService.checkout(request));
    }

    @PostMapping("/list")
    public ApiResponse<List<OrderDtos.OrderResponse>> orders(@RequestHeader("Authorization") String authorization) {
        authService.requireAuthorization(authorization);
        return new ApiResponse<>("orders", "success", orderService.fetchOrders());
    }

    @PostMapping("/detail")
    public ApiResponse<OrderDtos.OrderResponse> order(
            @RequestHeader("Authorization") String authorization,
            @RequestBody OrderDetailRequest request) {
        authService.requireAuthorization(authorization);
        return new ApiResponse<>("order-detail", "success", orderService.fetchOrder(request.orderId()));
    }

    public record OrderDetailRequest(
            String orderId
    ) {
    }
}