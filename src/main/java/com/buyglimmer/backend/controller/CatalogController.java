package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiResponse;
import com.buyglimmer.backend.dto.CatalogDtos;
import com.buyglimmer.backend.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CatalogDtos.CategoryResponse>> categories() {
        return new ApiResponse<>("categories", "success", catalogService.fetchCategories());
    }

    @GetMapping("/products")
    public ApiResponse<List<CatalogDtos.ProductResponse>> products(@RequestParam(required = false) String category) {
        return new ApiResponse<>("products", "success", catalogService.fetchProducts(category));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<CatalogDtos.ProductResponse> product(@PathVariable String productId) {
        return new ApiResponse<>("product-detail", "success", catalogService.fetchProduct(productId));
    }
}