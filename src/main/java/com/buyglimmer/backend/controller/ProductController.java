package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.ProductService;
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
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final AuthService authService;
    private final ProductService productService;
    private final ApiResponseFactory apiResponseFactory;

    public ProductController(AuthService authService, ProductService productService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.productService = productService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/list")
    public ApiWrapperResponse<List<FintechDtos.ProductSummaryResponse>> listProducts(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.ProductListRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/products/list requestId={}", request.requestId());
        return apiResponseFactory.success(
                request.requestId(),
                "Products fetched successfully",
                productService.listProducts(request.data())
        );
    }

    @PostMapping("/detail")
    public ApiWrapperResponse<FintechDtos.ProductDetailResponse> productDetail(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.ProductDetailRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/products/detail requestId={}", request.requestId());
        return apiResponseFactory.success(
                request.requestId(),
                "Product fetched successfully",
                productService.getProduct(request.data())
        );
    }

    @PostMapping("/search")
    public ApiWrapperResponse<List<FintechDtos.ProductSummaryResponse>> searchProducts(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.ProductSearchRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/products/search requestId={}", request.requestId());
        return apiResponseFactory.success(
                request.requestId(),
                "Products search completed",
                productService.searchProducts(request.data())
        );
    }
}
