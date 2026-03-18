package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class OrderProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderProcedureRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.OrderSummaryResponse createOrder(FintechDtos.OrderCreateRequest request) {
    String orderId = UUID.randomUUID().toString();
    String meta = "{\"coupon\":\"" + (request.couponCode() == null ? "" : request.couponCode())
        + "\",\"paymentMethod\":\"" + request.paymentMethod() + "\"}";
    jdbcTemplate.update("""
            INSERT INTO orders(id, customer_id, address_id, total_amount, status, payment_status, meta, created_at)
            VALUES (?, ?, ?, 0, 'pending', 'pending', ?, CURRENT_TIMESTAMP)
            """,
        orderId, request.customerId(), request.addressId(), meta);
    return getOrderSummary(orderId);
    }

    public void addOrderItem(String orderId, FintechDtos.OrderItemInput itemInput) {
    BigDecimal total = itemInput.price().multiply(BigDecimal.valueOf(itemInput.quantity()));
    jdbcTemplate.update("""
            INSERT INTO order_item(id, order_id, variant_id, qty, price, total)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
        UUID.randomUUID().toString(),
        orderId,
        itemInput.variantId(),
        itemInput.quantity(),
        itemInput.price(),
        total);

    jdbcTemplate.update("""
            UPDATE orders
            SET total_amount = (SELECT COALESCE(SUM(total), 0) FROM order_item WHERE order_id = ?)
            WHERE id = ?
            """,
        orderId, orderId);
    }

    public List<FintechDtos.OrderSummaryResponse> getOrders(String customerId) {
    return jdbcTemplate.query("""
            SELECT id AS order_id,
                   customer_id,
                   total_amount,
                   status,
                   payment_status,
                   created_at
            FROM orders
            WHERE customer_id = ?
            ORDER BY created_at DESC
            """,
        ps -> ps.setString(1, customerId),
        (rs, rowNum) -> new FintechDtos.OrderSummaryResponse(
                        rs.getString("order_id"),
                        rs.getString("customer_id"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status"),
                        rs.getString("payment_status"),
            toStringSafe(rs.getTimestamp("created_at"))
                ));
    }

    public List<FintechDtos.OrderItemResponse> getOrderItems(String orderId) {
    return jdbcTemplate.query("""
            SELECT oi.id AS order_item_id,
                   oi.variant_id,
                   p.name AS product_name,
                   oi.qty AS quantity,
                   oi.price,
                   oi.total
            FROM order_item oi
            LEFT JOIN product_variant pv ON pv.id = oi.variant_id
            LEFT JOIN product p ON p.id = pv.product_id
            WHERE oi.order_id = ?
            ORDER BY oi.id
            """,
        ps -> ps.setString(1, orderId),
        (rs, rowNum) -> new FintechDtos.OrderItemResponse(
                        rs.getString("order_item_id"),
                        rs.getString("variant_id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("total")
                ));
    }

    public FintechDtos.OrderSummaryResponse getOrderSummary(String orderId) {
        List<FintechDtos.OrderSummaryResponse> rows = jdbcTemplate.query("""
                        SELECT id AS order_id,
                               customer_id,
                               total_amount,
                               status,
                               payment_status,
                               created_at
                        FROM orders
                        WHERE id = ?
                        """,
                ps -> ps.setString(1, orderId),
                        (rs, rowNum) -> new FintechDtos.OrderSummaryResponse(
                        rs.getString("order_id"),
                        rs.getString("customer_id"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status"),
                        rs.getString("payment_status"),
                        toStringSafe(rs.getTimestamp("created_at"))
                ));
        if (rows.isEmpty()) {
            throw new java.util.NoSuchElementException("Order not found");
        }
        return rows.get(0);
    }

    private String toStringSafe(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toString();
    }
}
