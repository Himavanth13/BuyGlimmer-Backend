package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.CatalogDtos;
import com.buyglimmer.backend.repository.CatalogStoredProcedureRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final CatalogStoredProcedureRepository catalogRepository;

    public CatalogService(CatalogStoredProcedureRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    public List<CatalogDtos.ProductResponse> fetchProducts(String category) {
        return catalogRepository.fetchProducts(category);
    }

    public CatalogDtos.ProductResponse fetchProduct(String productId) {
        return catalogRepository.fetchProduct(productId);
    }

    public List<CatalogDtos.CategoryResponse> fetchCategories() {
        return catalogRepository.fetchCategories();
    }
}