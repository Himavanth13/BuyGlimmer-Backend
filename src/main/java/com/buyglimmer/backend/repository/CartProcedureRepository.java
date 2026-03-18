package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.util.DbCallUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CartProcedureRepository {

    private final DbCallUtils dbCallUtils;

    public CartProcedureRepository(DbCallUtils dbCallUtils) {
        this.dbCallUtils = dbCallUtils;
    }

    public FintechDtos.CartItemResponse addToCart(FintechDtos.CartAddRequest request) {
        return dbCallUtils.callForObject("{call sp_add_to_cart(?,?,?,?)}",
                cs -> {
                    cs.setString(1, request.customerId());
                    cs.setString(2, request.productId());
                    cs.setString(3, request.variantId());
                    cs.setInt(4, request.quantity());
                },
                rs -> new FintechDtos.CartItemResponse(
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

    public List<FintechDtos.CartItemResponse> getCart(String customerId) {
        return dbCallUtils.callForList("{call sp_get_cart(?)}",
                cs -> cs.setString(1, customerId),
                rs -> new FintechDtos.CartItemResponse(
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
        return dbCallUtils.callForUpdateCount("{call sp_update_cart_item(?,?)}",
                cs -> {
                    cs.setString(1, request.cartItemId());
                    cs.setInt(2, request.quantity());
                });
    }

    public int removeCartItem(String cartItemId) {
        return dbCallUtils.callForUpdateCount("{call sp_remove_cart_item(?)}", cs -> cs.setString(1, cartItemId));
    }
}
