package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiResponse;
import com.buyglimmer.backend.dto.CartDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final AuthService authService;
    private final CartService cartService;

    public CartController(AuthService authService, CartService cartService) {
        this.authService = authService;
        this.cartService = cartService;
    }

    @PostMapping("/list")
    public ApiResponse<List<CartDtos.CartItemResponse>> cart(@RequestHeader("Authorization") String authorization) {
        authService.requireAuthorization(authorization);
        return new ApiResponse<>("cart", "success", cartService.fetchCart());
    }

    @PostMapping("/items")
    public ApiResponse<CartDtos.CartItemResponse> addItem(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CartDtos.CartItemRequest request) {
        authService.requireAuthorization(authorization);
        return new ApiResponse<>("cart-add", "success", cartService.addItem(request));
    }

    @PostMapping("/items/remove")
    public ApiResponse<Void> removeItem(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody RemoveCartItemRequest request) {
        authService.requireAuthorization(authorization);
        cartService.removeItem(request.cartItemId());
        return new ApiResponse<>("cart-remove", "success", null);
    }

    public record RemoveCartItemRequest(
            String cartItemId
    ) {
    }
}