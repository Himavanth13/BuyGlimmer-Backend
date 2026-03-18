package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.util.DbCallUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductProcedureRepository {

    private final DbCallUtils dbCallUtils;

    public ProductProcedureRepository(DbCallUtils dbCallUtils) {
        this.dbCallUtils = dbCallUtils;
    }

    public List<FintechDtos.ProductSummaryResponse> getProducts() {
        return dbCallUtils.callForList("{call sp_get_products()}", null,
                rs -> new FintechDtos.ProductSummaryResponse(
                        rs.getString("product_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getString("description"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("mrp"),
                        rs.getInt("stock")
                ));
    }

    public FintechDtos.ProductDetailResponse getProduct(String productId) {
        return dbCallUtils.callForObject("{call sp_get_product(?)}",
                cs -> cs.setString(1, productId),
                rs -> new FintechDtos.ProductDetailResponse(
                        rs.getString("product_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getString("description"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("mrp"),
                        rs.getInt("stock"),
                        rs.getString("sku"),
                        rs.getString("image_url")
                ));
    }

    public List<FintechDtos.ProductSummaryResponse> searchProducts(String keyword) {
        return dbCallUtils.callForList("{call sp_search_products(?)}",
                cs -> cs.setString(1, keyword),
                rs -> new FintechDtos.ProductSummaryResponse(
                        rs.getString("product_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getString("description"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("mrp"),
                        rs.getInt("stock")
                ));
    }
}
