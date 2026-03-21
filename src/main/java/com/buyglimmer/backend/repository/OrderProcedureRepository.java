package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public class OrderProcedureRepository {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcedureRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public OrderProcedureRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.OrderSummaryResponse createOrder(FintechDtos.OrderCreateRequest request) {
    try {
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
    } catch (DataAccessException ex) {
        logger.warn("sp_create_order failed; using SQL fallback. customerId={}", request.customerId(), ex);
        String orderId = UUID.randomUUID().toString();
        String coupon = request.couponCode() == null ? "" : request.couponCode();
        String meta = String.format("{\"coupon\":\"%s\",\"paymentMethod\":\"%s\"}", coupon, request.paymentMethod());

        jdbcTemplate.update(
            "INSERT INTO orders(id, customer_id, address_id, total_amount, status, payment_status, meta, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
            orderId,
            request.customerId(),
            request.addressId(),
            BigDecimal.ZERO,
            "pending",
            "pending",
            meta
        );

        return jdbcTemplate.queryForObject(
            "SELECT id AS order_id, customer_id, total_amount, status, payment_status, CAST(created_at AS CHAR) AS created_at " +
                "FROM orders WHERE id = ?",
            (rs, rowNum) -> new FintechDtos.OrderSummaryResponse(
                rs.getString("order_id"),
                rs.getString("customer_id"),
                rs.getBigDecimal("total_amount"),
                rs.getString("status"),
                rs.getString("payment_status"),
                rs.getString("created_at")
            ),
            orderId
        );
    }
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

    public int clearActiveCartForCustomer(String customerId) {
        int removed = jdbcTemplate.update(
            "DELETE ci FROM cart_item ci " +
                "JOIN cart c ON c.id = ci.cart_id " +
                "WHERE c.customer_id = ? AND LOWER(c.status) = 'active'",
            customerId
        );

        jdbcTemplate.update(
            "UPDATE cart SET status = 'ordered' WHERE customer_id = ? AND LOWER(status) = 'active'",
            customerId
        );

        return removed;
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
