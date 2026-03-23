package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CartProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public CartProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.CartItemResponse addToCart(FintechDtos.CartAddRequest request, String actorId) {
        List<FintechDtos.CartItemResponse> rows = jdbcTemplate.query("CALL sp_add_to_cart(?, ?, ?, ?)",
            ps -> {
                ps.setString(1, actorId);
                ps.setString(2, request.productId());
                ps.setString(3, request.variantId());
                ps.setInt(4, request.quantity());
            },
                (rs, rowNum) -> new FintechDtos.CartItemResponse(
                        rs.getString("cart_item_id"),
                        rs.getString("customer_id"),
                        rs.getString("product_id"),
                        rs.getString("variant_id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        rs.getBigDecimal("line_total")
                ));
        if (rows.isEmpty()) {
            throw new java.util.NoSuchElementException("Cart item not found after insert");
        }
        return rows.get(0);
    }

    public List<FintechDtos.CartItemResponse> getCart(String customerId) {
        return jdbcTemplate.query("CALL sp_get_cart(?)",
        ps -> ps.setString(1, customerId),
                (rs, rowNum) -> new FintechDtos.CartItemResponse(
                        rs.getString("cart_item_id"),
                        rs.getString("customer_id"),
                        rs.getString("product_id"),
                        rs.getString("variant_id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        rs.getBigDecimal("line_total")
                ));
    }

    public int updateCartItem(FintechDtos.CartUpdateRequest request, String actorId) {
        Integer rows = jdbcTemplate.queryForObject(
            "CALL sp_update_cart_item(?, ?, ?)",
            Integer.class,
            request.cartItemId(),
            request.quantity(),
            actorId
        );
        return rows == null ? 0 : rows;
    }

    public int removeCartItem(String cartItemId, String actorId) {
        Integer rows = jdbcTemplate.queryForObject(
            "CALL sp_remove_cart_item(?, ?)",
            Integer.class,
            cartItemId,
            actorId
        );
        return rows == null ? 0 : rows;
    }

    public int mergeGuestCartIntoCustomer(String guestId, String customerId) {
        Integer rows = jdbcTemplate.queryForObject(
                "CALL sp_merge_guest_cart(?, ?)",
                Integer.class,
                guestId,
                customerId
        );
        return rows == null ? 0 : rows;
    }
}
