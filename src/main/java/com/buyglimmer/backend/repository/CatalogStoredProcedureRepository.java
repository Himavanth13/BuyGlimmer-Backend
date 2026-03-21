package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.CatalogDtos;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Repository
public class CatalogStoredProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public CatalogStoredProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CatalogDtos.CategoryResponse> fetchCategories() {
        return jdbcTemplate.query("select * from sp_fetch_categories()",
                (rs, rowNum) -> new CatalogDtos.CategoryResponse(rs.getString("name"), rs.getLong("product_count")));
    }

    public List<CatalogDtos.ProductResponse> fetchProducts(String category) {
        List<BaseProduct> baseProducts = jdbcTemplate.query("select * from sp_fetch_products(?)", ps -> setCategory(ps, category),
                (rs, rowNum) -> new BaseProduct(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price"),
                        rs.getString("category"),
                        rs.getString("description")
                ));

        return baseProducts.stream().map(this::hydrate).toList();
    }

    public CatalogDtos.ProductResponse fetchProduct(String productId) {
        try {
            List<BaseProduct> products = jdbcTemplate.query("select * from sp_fetch_product_by_id(?)",
                    ps -> ps.setString(1, productId),
                    (rs, rowNum) -> new BaseProduct(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getBigDecimal("price"),
                            rs.getString("category"),
                            rs.getString("description")
                    ));

            if (products.isEmpty()) {
                throw new NoSuchElementException("Product not found for id " + productId);
            }

            return hydrate(products.get(0));
        } catch (DataAccessException ex) {
            List<BaseProduct> products = jdbcTemplate.query(
                    """
                    select p.id,
                           p.name,
                           coalesce(min(v.price), 0) as price,
                           coalesce(max(c.name), '') as category,
                           p.description
                    from product p
                    left join product_variant v on v.product_id = p.id
                    left join product_category pc on pc.product_id = p.id
                    left join category c on c.id = pc.category_id
                    where p.id = ?
                    group by p.id, p.name, p.description
                    """,
                    ps -> ps.setString(1, productId),
                    (rs, rowNum) -> new BaseProduct(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getBigDecimal("price"),
                            rs.getString("category"),
                            rs.getString("description")
                    )
            );

            if (products.isEmpty()) {
                throw new NoSuchElementException("Product not found for id " + productId);
            }

            BaseProduct product = products.get(0);
            return new CatalogDtos.ProductResponse(
                    product.id(),
                    product.name(),
                    product.price(),
                    product.category(),
                    Collections.emptyList(),
                    product.description(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyMap(),
                    Collections.emptyList()
            );
        }
    }

    private CatalogDtos.ProductResponse hydrate(BaseProduct baseProduct) {
        List<String> images = jdbcTemplate.query("select * from sp_fetch_product_images(?)",
                ps -> ps.setString(1, baseProduct.id()),
                (rs, rowNum) -> rs.getString("image_url"));

        List<String> sizes = jdbcTemplate.query("select * from sp_fetch_product_sizes(?)",
                ps -> ps.setString(1, baseProduct.id()),
                (rs, rowNum) -> rs.getString("size_value"));

        List<CatalogDtos.ColorOptionResponse> colors = jdbcTemplate.query("select * from sp_fetch_product_colors(?)",
                ps -> ps.setString(1, baseProduct.id()),
                (rs, rowNum) -> new CatalogDtos.ColorOptionResponse(
                        rs.getString("color_name"),
                        rs.getString("hex_code"),
                        rs.getString("image_url")
                ));

        Map<String, String> specs = new LinkedHashMap<>();
        List<SpecRow> specRows = jdbcTemplate.query("select * from sp_fetch_product_specs(?)",
                ps -> ps.setString(1, baseProduct.id()),
                (rs, rowNum) -> new SpecRow(rs.getString("spec_key"), rs.getString("spec_value")));
        specRows.forEach(spec -> specs.put(spec.key(), spec.value()));

        List<CatalogDtos.ReviewResponse> reviews = jdbcTemplate.query("select * from sp_fetch_product_reviews(?)",
                ps -> ps.setString(1, baseProduct.id()),
                (rs, rowNum) -> new CatalogDtos.ReviewResponse(
                        rs.getString("user_name"),
                        rs.getInt("rating"),
                        rs.getString("comment_text"),
                        rs.getDate("review_date").toLocalDate().toString()
                ));

        return new CatalogDtos.ProductResponse(
                baseProduct.id(),
                baseProduct.name(),
                baseProduct.price(),
                baseProduct.category(),
                images,
                baseProduct.description(),
                sizes,
                colors,
                specs,
                reviews
        );
    }

        private void setCategory(PreparedStatement statement, String category) throws java.sql.SQLException {
        if (category == null || category.isBlank() || "All".equalsIgnoreCase(category)) {
            statement.setNull(1, Types.VARCHAR);
            return;
        }
        statement.setString(1, category);
    }

    private record BaseProduct(
            String id,
            String name,
            java.math.BigDecimal price,
            String category,
            String description
    ) {
    }

    private record SpecRow(
            String key,
            String value
    ) {
    }
}