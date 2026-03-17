package com.buyglimmer.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CartStoredProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public CartStoredProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CartLine> fetchCart(String userId) {
        return jdbcTemplate.query("select * from sp_fetch_cart(?)",
                ps -> ps.setString(1, userId),
                (rs, rowNum) -> new CartLine(
                        rs.getString("cart_item_id"),
                rs.getString("product_id"),
                        rs.getInt("quantity"),
                        rs.getString("selected_size"),
                        rs.getString("selected_color")
                ));
    }

        public CartLine addItem(String userId, String productId, Integer quantity, String selectedSize, String selectedColor) {
        String cartItemId = jdbcTemplate.queryForObject(
                "call sp_add_cart_item(?, ?, ?, ?, ?)",
                String.class,
                userId,
                productId,
                quantity,
                selectedSize,
                selectedColor
        );
        return fetchCart(userId).stream()
                .filter(line -> line.cartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow();
    }

    public boolean removeItem(String cartItemId) {
        Integer affected = jdbcTemplate.queryForObject("call sp_delete_cart_item(?)", Integer.class, cartItemId);
        return affected != null && affected > 0;
    }

    public void clearCart(String userId) {
        jdbcTemplate.queryForObject("call sp_clear_cart(?)", Integer.class, userId);
    }

    public record CartLine(
            String cartItemId,
            String productId,
            Integer quantity,
            String selectedSize,
            String selectedColor
    ) {
    }
}