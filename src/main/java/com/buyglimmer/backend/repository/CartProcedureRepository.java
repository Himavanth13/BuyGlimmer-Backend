package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public class CartProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public CartProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.CartItemResponse addToCart(FintechDtos.CartAddRequest request) {
        String cartId = ensureActiveCart(request.customerId());
        String variantId = request.variantId();
        if (variantId == null || variantId.isBlank()) {
            List<String> variants = jdbcTemplate.query("""
                            SELECT id FROM product_variant
                            WHERE product_id = ?
                            ORDER BY price
                            LIMIT 1
                            """,
                    ps -> ps.setString(1, request.productId()),
                    (rs, rowNum) -> rs.getString("id"));
            if (variants.isEmpty()) {
                throw new java.util.NoSuchElementException("No product variant found");
            }
            variantId = variants.get(0);
        }

        BigDecimal unitPrice = jdbcTemplate.queryForObject(
                "SELECT price FROM product_variant WHERE id = ?",
                BigDecimal.class,
                variantId
        );
        String cartItemId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                        INSERT INTO cart_item(id, cart_id, variant_id, qty, price)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                cartItemId, cartId, variantId, request.quantity(), unitPrice);

        List<FintechDtos.CartItemResponse> rows = jdbcTemplate.query("""
                        SELECT ci.id AS cart_item_id,
                               c.customer_id,
                               pv.product_id,
                               ci.variant_id,
                               p.name AS product_name,
                               ci.qty AS quantity,
                               ci.price AS unit_price,
                               ci.price * ci.qty AS line_total
                        FROM cart_item ci
                        JOIN cart c ON c.id = ci.cart_id
                        JOIN product_variant pv ON pv.id = ci.variant_id
                        JOIN product p ON p.id = pv.product_id
                        WHERE ci.id = ?
                        """,
                ps -> ps.setString(1, cartItemId),
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
        return jdbcTemplate.query("""
            SELECT ci.id AS cart_item_id,
                   c.customer_id,
                   pv.product_id,
                   ci.variant_id,
                   p.name AS product_name,
                   ci.qty AS quantity,
                   ci.price AS unit_price,
                   ci.price * ci.qty AS line_total
            FROM cart c
            JOIN cart_item ci ON ci.cart_id = c.id
            JOIN product_variant pv ON pv.id = ci.variant_id
            JOIN product p ON p.id = pv.product_id
            WHERE c.customer_id = ? AND LOWER(c.status) = 'active'
            ORDER BY ci.id
            """,
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

    public int updateCartItem(FintechDtos.CartUpdateRequest request) {
        return jdbcTemplate.update(
                "UPDATE cart_item SET qty = ? WHERE id = ?",
                request.quantity(),
                request.cartItemId()
        );
    }

    public int removeCartItem(String cartItemId) {
        return jdbcTemplate.update("DELETE FROM cart_item WHERE id = ?", cartItemId);
    }

    private String ensureActiveCart(String customerId) {
        List<String> cartIds = jdbcTemplate.query(
                "SELECT id FROM cart WHERE customer_id = ? AND LOWER(status) = 'active' ORDER BY created_at DESC LIMIT 1",
                ps -> ps.setString(1, customerId),
                (rs, rowNum) -> rs.getString("id")
        );
        if (!cartIds.isEmpty()) {
            return cartIds.get(0);
        }
        String cartId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO cart(id, customer_id, status, created_at) VALUES (?, ?, 'active', CURRENT_TIMESTAMP)",
                cartId, customerId
        );
        return cartId;
    }
}
