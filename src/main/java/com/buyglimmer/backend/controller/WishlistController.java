package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiResponse;
import com.buyglimmer.backend.dto.CatalogDtos;
import com.buyglimmer.backend.dto.UserDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    private final AuthService authService;
    private final WishlistService wishlistService;

    public WishlistController(AuthService authService, WishlistService wishlistService) {
        this.authService = authService;
        this.wishlistService = wishlistService;
    }

    @PostMapping("/list")
    public ApiResponse<List<CatalogDtos.ProductResponse>> wishlist(@RequestHeader("Authorization") String authorization) {
        authService.requireAuthorization(authorization);
        return new ApiResponse<>("wishlist", "success", wishlistService.fetchWishlist());
    }

    @PostMapping("/toggle")
    public ApiResponse<List<CatalogDtos.ProductResponse>> toggle(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UserDtos.WishlistToggleRequest request) {
        authService.requireAuthorization(authorization);
        return new ApiResponse<>("wishlist-toggle", "success", wishlistService.toggleProduct(request.productId()));
    }
}