package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.CatalogDtos;
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

    public List<CatalogDtos.ProductResponse> fetchWishlist() {
        return wishlistRepository.fetchWishlistProductIds(UserService.DEFAULT_USER_ID).stream()
                .map(catalogService::fetchProduct)
                .toList();
    }

    public List<CatalogDtos.ProductResponse> toggleProduct(String productId) {
        catalogService.fetchProduct(productId);
        wishlistRepository.toggleWishlist(UserService.DEFAULT_USER_ID, productId);
        return fetchWishlist();
    }
}