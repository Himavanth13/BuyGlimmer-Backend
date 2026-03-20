package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductProcedureRepository {

        private final JdbcTemplate jdbcTemplate;

        public ProductProcedureRepository(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
    }

    public List<FintechDtos.ProductSummaryResponse> getProducts() {
                return jdbcTemplate.query("CALL sp_get_products()",
                (rs, rowNum) -> new FintechDtos.ProductSummaryResponse(
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
        List<FintechDtos.ProductDetailResponse> rows = jdbcTemplate.query("CALL sp_get_product(?)",
                ps -> ps.setString(1, productId),
                (rs, rowNum) -> new FintechDtos.ProductDetailResponse(
                        rs.getString("product_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getString("description"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("mrp"),
                        rs.getInt("stock"),
                                                rs.getString("sku"),
                                                firstImage(rs.getString("images"))
                ));

                if (rows.isEmpty()) {
                        throw new java.util.NoSuchElementException("Product not found");
                }
                return rows.get(0);
    }

    public List<FintechDtos.ProductSummaryResponse> searchProducts(String keyword) {
                return jdbcTemplate.query("CALL sp_search_products(?)",
                                ps -> ps.setString(1, keyword),
                (rs, rowNum) -> new FintechDtos.ProductSummaryResponse(
                        rs.getString("product_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getString("description"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("mrp"),
                        rs.getInt("stock")
                ));
    }

        private String firstImage(String images) {
                if (images == null || images.isBlank()) {
                        return null;
                }
                String trimmed = images.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
                        if (inner.startsWith("\"") && inner.endsWith("\"") && inner.length() >= 2) {
                                return inner.substring(1, inner.length() - 1);
                        }
                        return inner;
                }
                return trimmed;
        }
}
