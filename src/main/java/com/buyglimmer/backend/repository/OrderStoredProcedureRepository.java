package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.OrderDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.NoSuchElementException;

@Repository
public class OrderStoredProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderStoredProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public OrderDtos.OrderResponse createOrder(String userId, OrderDtos.CheckoutRequest request) {
        String addressId = jdbcTemplate.queryForObject(
                "call sp_upsert_address(?, ?, ?, ?, ?, ?, ?)",
                String.class,
                userId,
                request.shippingAddress().name(),
                request.shippingAddress().address(),
                request.shippingAddress().city(),
                request.shippingAddress().state(),
                request.shippingAddress().pincode(),
                request.shippingAddress().phone()
        );
        String orderId = jdbcTemplate.queryForObject("call sp_create_order(?, ?)", String.class, userId, addressId);
        jdbcTemplate.queryForObject("call sp_transfer_cart_to_order(?, ?, ?)", Integer.class, userId, orderId, request.paymentMethod());
        return fetchOrder(orderId);
    }

    public List<OrderDtos.OrderResponse> fetchOrders(String userId) {
        List<OrderRow> rows = jdbcTemplate.query("select * from sp_fetch_orders(?)",
                ps -> ps.setString(1, userId),
                (rs, rowNum) -> new OrderRow(
                        rs.getString("id"),
                        rs.getDate("order_date").toLocalDate().toString(),
                        rs.getString("status"),
                        rs.getBigDecimal("total"),
                        rs.getString("payment_method"),
                        new OrderDtos.ShippingAddressRequest(
                                rs.getString("shipping_name"),
                                rs.getString("shipping_address"),
                                rs.getString("city"),
                                rs.getString("state"),
                                rs.getString("pincode"),
                                rs.getString("phone"),
                                rs.getString("email")
                        ),
                        new OrderDtos.TrackingDetailsResponse(
                                rs.getString("tracking_number"),
                                rs.getString("carrier"),
                                rs.getDate("estimated_delivery").toLocalDate().toString()
                        )
                ));

        return rows.stream().map(this::hydrate).toList();
    }

    public OrderDtos.OrderResponse fetchOrder(String orderId) {
        List<OrderRow> rows = jdbcTemplate.query("select * from sp_fetch_order_by_id(?)",
                ps -> ps.setString(1, orderId),
                (rs, rowNum) -> new OrderRow(
                        rs.getString("id"),
                        rs.getDate("order_date").toLocalDate().toString(),
                        rs.getString("status"),
                        rs.getBigDecimal("total"),
                        rs.getString("payment_method"),
                        new OrderDtos.ShippingAddressRequest(
                                rs.getString("shipping_name"),
                                rs.getString("shipping_address"),
                                rs.getString("city"),
                                rs.getString("state"),
                                rs.getString("pincode"),
                                rs.getString("phone"),
                                rs.getString("email")
                        ),
                        new OrderDtos.TrackingDetailsResponse(
                                rs.getString("tracking_number"),
                                rs.getString("carrier"),
                                rs.getDate("estimated_delivery").toLocalDate().toString()
                        )
                ));

        if (rows.isEmpty()) {
            throw new NoSuchElementException("Order not found for id " + orderId);
        }
        return hydrate(rows.get(0));
    }

    private OrderDtos.OrderResponse hydrate(OrderRow row) {
        List<OrderDtos.OrderItemResponse> items = jdbcTemplate.query("select * from sp_fetch_order_items(?)",
                ps -> ps.setString(1, row.id()),
                (rs, rowNum) -> new OrderDtos.OrderItemResponse(
                        rs.getString("name"),
                        rs.getBigDecimal("price"),
                        rs.getString("image"),
                        rs.getString("color"),
                        rs.getInt("quantity")
                ));

        return new OrderDtos.OrderResponse(
                row.id(),
                row.date(),
                row.status(),
                items,
                row.total(),
                row.paymentMethod(),
                row.shippingAddress(),
                row.tracking()
        );
    }

    private record OrderRow(
            String id,
            String date,
            String status,
            java.math.BigDecimal total,
            String paymentMethod,
            OrderDtos.ShippingAddressRequest shippingAddress,
            OrderDtos.TrackingDetailsResponse tracking
    ) {
    }
}