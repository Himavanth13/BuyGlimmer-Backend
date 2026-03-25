package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.CartProcedureService;
import com.buyglimmer.backend.util.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${cart.module.api.base-path}")
public class CartProcedureController {

    private static final Logger logger = LoggerFactory.getLogger(CartProcedureController.class);

    private final CartProcedureService cartProcedureService;
    private final ApiResponseFactory apiResponseFactory;
    private final String addSuccessMessage;
    private final String getSuccessMessage;
    private final String updateSuccessMessage;
    private final String removeSuccessMessage;

    public CartProcedureController(
            CartProcedureService cartProcedureService,
            ApiResponseFactory apiResponseFactory,
            @Value("${cart.module.message.add-success}") String addSuccessMessage,
            @Value("${cart.module.message.get-success}") String getSuccessMessage,
            @Value("${cart.module.message.update-success}") String updateSuccessMessage,
            @Value("${cart.module.message.remove-success}") String removeSuccessMessage
    ) {
        this.cartProcedureService = cartProcedureService;
        this.apiResponseFactory = apiResponseFactory;
        this.addSuccessMessage = addSuccessMessage;
        this.getSuccessMessage = getSuccessMessage;
        this.updateSuccessMessage = updateSuccessMessage;
        this.removeSuccessMessage = removeSuccessMessage;
    }

    @PostMapping("${cart.module.api.add-path}")
    public ApiWrapperResponse<FintechDtos.CartItemResponse> addToCart(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.CartAddRequest> request) {
        logger.info("Cart add request requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), addSuccessMessage, cartProcedureService.addToCart(request.data()));
    }

    @PostMapping("${cart.module.api.get-path}")
    public ApiWrapperResponse<List<FintechDtos.CartItemResponse>> getCart(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.CartGetRequest> request) {
        logger.info("Cart get request requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), getSuccessMessage, cartProcedureService.getCart(request.data()));
    }

    @PostMapping("${cart.module.api.update-path}")
    public ApiWrapperResponse<Object> updateCart(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.CartUpdateRequest> request) {
        logger.info("Cart update request requestId={}", request.requestId());
        cartProcedureService.updateCartItem(request.data());
        return apiResponseFactory.success(request.requestId(), updateSuccessMessage, null);
    }

    @PostMapping("${cart.module.api.remove-path}")
    public ApiWrapperResponse<Object> removeCart(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.CartRemoveRequest> request) {
        logger.info("Cart remove request requestId={}", request.requestId());
        cartProcedureService.removeCartItem(request.data());
        return apiResponseFactory.success(request.requestId(), removeSuccessMessage, null);
    }
}
