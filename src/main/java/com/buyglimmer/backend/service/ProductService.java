package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.repository.ProductProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductProcedureRepository productProcedureRepository;

    public ProductService(ProductProcedureRepository productProcedureRepository) {
        this.productProcedureRepository = productProcedureRepository;
    }

    public List<FintechDtos.ProductSummaryResponse> listProducts(FintechDtos.ProductListRequest request) {
        logger.info("Fetching products list");
        return productProcedureRepository.getProducts();
    }

    public FintechDtos.ProductDetailResponse getProduct(FintechDtos.ProductDetailRequest request) {
        logger.info("Fetching product detail for productId={}", request.productId());
        return productProcedureRepository.getProduct(request.productId());
    }

    public List<FintechDtos.ProductSummaryResponse> searchProducts(FintechDtos.ProductSearchRequest request) {
        logger.info("Searching products with keyword={}", request.keyword());
        return productProcedureRepository.searchProducts(request.keyword());
    }
}
