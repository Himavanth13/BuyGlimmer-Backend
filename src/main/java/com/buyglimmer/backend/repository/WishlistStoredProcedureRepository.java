package com.buyglimmer.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WishlistStoredProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public WishlistStoredProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> fetchWishlistProductIds(String userId) {
        return jdbcTemplate.query("select * from sp_fetch_wishlist(?)",
                ps -> ps.setString(1, userId),
                (rs, rowNum) -> rs.getString("product_id"));
    }

    public void toggleWishlist(String userId, String productId) {
        jdbcTemplate.queryForObject("call sp_toggle_wishlist(?, ?)", Integer.class, userId, productId);
    }
}