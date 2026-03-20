package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderProcedureRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.OrderSummaryResponse createOrder(FintechDtos.OrderCreateRequest request) {
    List<FintechDtos.OrderSummaryResponse> rows = jdbcTemplate.query("CALL sp_create_order(?, ?, ?, ?)",
        ps -> {
            ps.setString(1, request.customerId());
            ps.setString(2, request.addressId());
            ps.setString(3, request.couponCode());
            ps.setString(4, request.paymentMethod());
        },
        (rs, rowNum) -> new FintechDtos.OrderSummaryResponse(
            rs.getString("order_id"),
            rs.getString("customer_id"),
            rs.getBigDecimal("total_amount"),
            rs.getString("status"),
            rs.getString("payment_status"),
            rs.getString("created_at")
        ));
    if (rows.isEmpty()) {
        throw new java.util.NoSuchElementException("Order not created");
    }
    return rows.get(0);
    }

    public void addOrderItem(String orderId, FintechDtos.OrderItemInput itemInput) {
    jdbcTemplate.queryForObject(
        "CALL sp_add_order_items(?, ?, ?, ?)",
        String.class,
        orderId,
        itemInput.variantId(),
        itemInput.quantity(),
        itemInput.price()
    );
    }

    public List<FintechDtos.OrderSummaryResponse> getOrders(String customerId) {
    return jdbcTemplate.query("CALL sp_get_orders(?)",
        ps -> ps.setString(1, customerId),
        (rs, rowNum) -> new FintechDtos.OrderSummaryResponse(
                        rs.getString("order_id"),
                        rs.getString("customer_id"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status"),
                        rs.getString("payment_status"),
            rs.getString("created_at")
                ));
    }

    public List<FintechDtos.OrderItemResponse> getOrderItems(String orderId) {
    return jdbcTemplate.query("CALL sp_get_order_detail(?)",
        ps -> ps.setString(1, orderId),
        (rs, rowNum) -> new FintechDtos.OrderItemResponse(
                        rs.getString("order_item_id"),
                        rs.getString("variant_id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("total")
                ))
            .stream()
            .filter(item -> item.orderItemId() != null)
            .toList();
    }

    public FintechDtos.OrderSummaryResponse getOrderSummary(String orderId) {
        List<FintechDtos.OrderSummaryResponse> rows = jdbcTemplate.query("CALL sp_get_order_detail(?)",
                ps -> ps.setString(1, orderId),
                        (rs, rowNum) -> new FintechDtos.OrderSummaryResponse(
                        rs.getString("order_id"),
                        rs.getString("customer_id"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status"),
                        rs.getString("payment_status"),
                        rs.getString("created_at")
                ));
        if (rows.isEmpty()) {
            throw new java.util.NoSuchElementException("Order not found");
        }
        return rows.get(0);
    }
}
