package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.CatalogDtos;
import com.buyglimmer.backend.dto.UserDtos;
import com.buyglimmer.backend.repository.WishlistStoredProcedureRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishlistService {

    private final CatalogService catalogService;
    private final WishlistStoredProcedureRepository wishlistRepository;

    public WishlistService(CatalogService catalogService, WishlistStoredProcedureRepository wishlistRepository) {
        this.catalogService = catalogService;
        this.wishlistRepository = wishlistRepository;
    }

    public List<CatalogDtos.ProductResponse> fetchWishlist(String customerId) {
        return wishlistRepository.fetchWishlistProductIds(resolveCustomerId(customerId)).stream()
                .map(catalogService::fetchProduct)
                .toList();
    }

    public List<CatalogDtos.ProductResponse> toggleProduct(UserDtos.WishlistToggleRequest request) {
        catalogService.fetchProduct(request.productId());
        String customerId = resolveCustomerId(request.customerId());
        wishlistRepository.toggleWishlist(customerId, request.productId());
        return fetchWishlist(customerId);
    }

    private String resolveCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return UserService.DEFAULT_USER_ID;
        }
        return customerId;
    }
}