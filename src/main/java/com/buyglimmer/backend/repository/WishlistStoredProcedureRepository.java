package com.buyglimmer.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class WishlistStoredProcedureRepository {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, Set<String>> inMemoryWishlist = new ConcurrentHashMap<>();

    public WishlistStoredProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> fetchWishlistProductIds(String userId) {
        try {
            return jdbcTemplate.query("select * from sp_fetch_wishlist(?)",
                    ps -> ps.setString(1, userId),
                    (rs, rowNum) -> rs.getString("product_id"));
        } catch (DataAccessException ex) {
            try {
                return jdbcTemplate.query("select product_id from wishlist where user_id = ?",
                        ps -> ps.setString(1, userId),
                        (rs, rowNum) -> rs.getString("product_id"));
            } catch (DataAccessException ignored) {
                return inMemoryWishlist.getOrDefault(userId, Set.of()).stream().toList();
            }
        }
    }

    public void toggleWishlist(String userId, String productId) {
        try {
            jdbcTemplate.queryForObject("call sp_toggle_wishlist(?, ?)", Integer.class, userId, productId);
            return;
        } catch (DataAccessException ex) {
            try {
                Integer exists = jdbcTemplate.queryForObject(
                        "select count(1) from wishlist where user_id = ? and product_id = ?",
                        Integer.class,
                        userId,
                        productId
                );
                if (exists != null && exists > 0) {
                    jdbcTemplate.update("delete from wishlist where user_id = ? and product_id = ?", userId, productId);
                } else {
                    jdbcTemplate.update("insert into wishlist(user_id, product_id) values (?, ?)", userId, productId);
                }
            } catch (DataAccessException ignored) {
                Set<String> productIds = inMemoryWishlist.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet());
                if (!productIds.add(productId)) {
                    productIds.remove(productId);
                }
            }
        }
    }
}