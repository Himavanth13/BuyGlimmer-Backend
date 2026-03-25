package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CartProcedureRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String addToCartProcedure;
    private final String getCartProcedure;
    private final String updateCartProcedure;
    private final String removeCartProcedure;
    private final String mergeGuestCartProcedure;
    private final String activeStatus;
    private final String mergedStatus;
    private final String itemNotFoundAfterInsertMessage;

    public CartProcedureRepository(
            JdbcTemplate jdbcTemplate,
            @Value("${cart.module.procedure.add-to-cart}") String addToCartProcedure,
            @Value("${cart.module.procedure.get-cart}") String getCartProcedure,
            @Value("${cart.module.procedure.update-cart-item}") String updateCartProcedure,
            @Value("${cart.module.procedure.remove-cart-item}") String removeCartProcedure,
            @Value("${cart.module.procedure.merge-guest-cart}") String mergeGuestCartProcedure,
            @Value("${cart.module.status.active}") String activeStatus,
            @Value("${cart.module.status.merged}") String mergedStatus,
            @Value("${cart.module.message.item-not-found-after-insert}") String itemNotFoundAfterInsertMessage
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.addToCartProcedure = addToCartProcedure;
        this.getCartProcedure = getCartProcedure;
        this.updateCartProcedure = updateCartProcedure;
        this.removeCartProcedure = removeCartProcedure;
        this.mergeGuestCartProcedure = mergeGuestCartProcedure;
        this.activeStatus = activeStatus;
        this.mergedStatus = mergedStatus;
        this.itemNotFoundAfterInsertMessage = itemNotFoundAfterInsertMessage;
    }

    public FintechDtos.CartItemResponse addToCart(FintechDtos.CartAddRequest request, String actorId) {
        List<FintechDtos.CartItemResponse> rows = jdbcTemplate.query(addToCartProcedure,
            ps -> {
                ps.setString(1, actorId);
                ps.setString(2, request.productId());
                ps.setString(3, request.variantId());
                ps.setInt(4, request.quantity());
                ps.setString(5, activeStatus);
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
            throw new java.util.NoSuchElementException(itemNotFoundAfterInsertMessage);
        }
        return rows.get(0);
    }

    public List<FintechDtos.CartItemResponse> getCart(String customerId) {
        return jdbcTemplate.query(getCartProcedure,
        ps -> {
            ps.setString(1, customerId);
            ps.setString(2, activeStatus);
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
    }

    public int updateCartItem(FintechDtos.CartUpdateRequest request, String actorId) {
        Integer rows = jdbcTemplate.queryForObject(
            updateCartProcedure,
            Integer.class,
            request.cartItemId(),
            request.quantity(),
            actorId,
            activeStatus
        );
        return rows == null ? 0 : rows;
    }

    public int removeCartItem(String cartItemId, String actorId) {
        Integer rows = jdbcTemplate.queryForObject(
            removeCartProcedure,
            Integer.class,
            cartItemId,
            actorId,
            activeStatus
        );
        return rows == null ? 0 : rows;
    }

    public int mergeGuestCartIntoCustomer(String guestId, String customerId) {
        Integer rows = jdbcTemplate.queryForObject(
                mergeGuestCartProcedure,
                Integer.class,
                guestId,
                customerId,
                activeStatus,
                mergedStatus
        );
        return rows == null ? 0 : rows;
    }
}
