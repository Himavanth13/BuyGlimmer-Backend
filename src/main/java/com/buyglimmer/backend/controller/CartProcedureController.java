package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.CartProcedureService;
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
@RequestMapping("/api/v1/cart")
public class CartProcedureController {

    private static final Logger logger = LoggerFactory.getLogger(CartProcedureController.class);

    private final AuthService authService;
    private final CartProcedureService cartProcedureService;
    private final ApiResponseFactory apiResponseFactory;

    public CartProcedureController(AuthService authService, CartProcedureService cartProcedureService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.cartProcedureService = cartProcedureService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/add")
    public ApiWrapperResponse<FintechDtos.CartItemResponse> addToCart(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.CartAddRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/cart/add requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Item added to cart", cartProcedureService.addToCart(request.data()));
    }

    @PostMapping("/get")
    public ApiWrapperResponse<List<FintechDtos.CartItemResponse>> getCart(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.CartGetRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/cart/get requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Cart fetched successfully", cartProcedureService.getCart(request.data()));
    }

    @PostMapping("/update")
    public ApiWrapperResponse<Object> updateCart(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.CartUpdateRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/cart/update requestId={}", request.requestId());
        cartProcedureService.updateCartItem(request.data());
        return apiResponseFactory.success(request.requestId(), "Cart item updated", null);
    }

    @PostMapping("/remove")
    public ApiWrapperResponse<Object> removeCart(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.CartRemoveRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/cart/remove requestId={}", request.requestId());
        cartProcedureService.removeCartItem(request.data());
        return apiResponseFactory.success(request.requestId(), "Cart item removed", null);
    }
}
