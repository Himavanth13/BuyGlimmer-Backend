package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiResponse;
import com.buyglimmer.backend.dto.CatalogDtos;
import com.buyglimmer.backend.service.CatalogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/categories")
    public ApiResponse<List<CatalogDtos.CategoryResponse>> categories() {
        return new ApiResponse<>("categories", "success", catalogService.fetchCategories());
    }

    @PostMapping("/products")
    public ApiResponse<List<CatalogDtos.ProductResponse>> products(@RequestBody(required = false) ProductListRequest request) {
        String category = request == null ? null : request.category();
        return new ApiResponse<>("products", "success", catalogService.fetchProducts(category));
    }

    @PostMapping("/products/detail")
    public ApiResponse<CatalogDtos.ProductResponse> product(@RequestBody ProductDetailRequest request) {
        return new ApiResponse<>("product-detail", "success", catalogService.fetchProduct(request.productId()));
    }

    public record ProductListRequest(
            String category
    ) {
    }

    public record ProductDetailRequest(
            String productId
    ) {
    }
}