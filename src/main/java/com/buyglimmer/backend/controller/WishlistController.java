package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.CatalogDtos;
import com.buyglimmer.backend.dto.UserDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.WishlistService;
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
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    private static final Logger logger = LoggerFactory.getLogger(WishlistController.class);

    private final AuthService authService;
    private final WishlistService wishlistService;
    private final ApiResponseFactory apiResponseFactory;

    public WishlistController(AuthService authService, WishlistService wishlistService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.wishlistService = wishlistService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/list")
    public ApiWrapperResponse<List<CatalogDtos.ProductResponse>> listWishlist(
            @Valid @RequestBody ApiWrapperRequest<UserDtos.WishlistFetchRequest> request) {
        String authenticatedCustomerId = authService.getAuthenticatedCustomerId(request.token());
        if (request.data().customerId() != null && !request.data().customerId().isBlank()) {
            authService.assertCustomerOwnership(request.token(), request.data().customerId());
        }
        logger.info("POST /api/v1/wishlist/list requestId={}", request.requestId());
        return apiResponseFactory.success(
                request.requestId(),
                "Wishlist fetched successfully",
            wishlistService.fetchWishlist(authenticatedCustomerId)
        );
    }

    @PostMapping("/toggle")
    public ApiWrapperResponse<List<CatalogDtos.ProductResponse>> toggleWishlist(
            @Valid @RequestBody ApiWrapperRequest<UserDtos.WishlistToggleRequest> request) {
        String authenticatedCustomerId = authService.getAuthenticatedCustomerId(request.token());
        if (request.data().customerId() != null && !request.data().customerId().isBlank()) {
            authService.assertCustomerOwnership(request.token(), request.data().customerId());
        }
        logger.info("POST /api/v1/wishlist/toggle requestId={}", request.requestId());
        UserDtos.WishlistToggleRequest ownedRequest = new UserDtos.WishlistToggleRequest(
            request.data().productId(),
            authenticatedCustomerId
        );
        return apiResponseFactory.success(
                request.requestId(),
                "Wishlist updated successfully",
            wishlistService.toggleProduct(ownedRequest)
        );
    }
}
