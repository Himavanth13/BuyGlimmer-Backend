package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.util.DbCallUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderProcedureRepository {

    private final DbCallUtils dbCallUtils;

    public OrderProcedureRepository(DbCallUtils dbCallUtils) {
        this.dbCallUtils = dbCallUtils;
    }

    public FintechDtos.OrderSummaryResponse createOrder(FintechDtos.OrderCreateRequest request) {
        return dbCallUtils.callForObject("{call sp_create_order(?,?,?,?)}",
                cs -> {
                    cs.setString(1, request.customerId());
                    cs.setString(2, request.addressId());
                    cs.setString(3, request.couponCode());
                    cs.setString(4, request.paymentMethod());
                },
                rs -> new FintechDtos.OrderSummaryResponse(
                        rs.getString("order_id"),
                        rs.getString("customer_id"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status"),
                        rs.getString("payment_status"),
                        rs.getString("created_at")
                ));
    }

    public void addOrderItem(String orderId, FintechDtos.OrderItemInput itemInput) {
        dbCallUtils.callForUpdateCount("{call sp_add_order_items(?,?,?,?)}",
                cs -> {
                    cs.setString(1, orderId);
                    cs.setString(2, itemInput.variantId());
                    cs.setInt(3, itemInput.quantity());
                    cs.setBigDecimal(4, itemInput.price());
                });
    }

    public List<FintechDtos.OrderSummaryResponse> getOrders(String customerId) {
        return dbCallUtils.callForList("{call sp_get_orders(?)}",
                cs -> cs.setString(1, customerId),
                rs -> new FintechDtos.OrderSummaryResponse(
                        rs.getString("order_id"),
                        rs.getString("customer_id"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status"),
                        rs.getString("payment_status"),
                        rs.getString("created_at")
                ));
    }

    public List<FintechDtos.OrderItemResponse> getOrderItems(String orderId) {
        return dbCallUtils.callForList("{call sp_get_order_detail(?)}",
                cs -> cs.setString(1, orderId),
                rs -> new FintechDtos.OrderItemResponse(
                        rs.getString("order_item_id"),
                        rs.getString("variant_id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("total")
                ));
    }

    public FintechDtos.OrderSummaryResponse getOrderSummary(String orderId) {
        return dbCallUtils.callForObject("{call sp_get_order_detail(?)}",
                cs -> cs.setString(1, orderId),
                rs -> new FintechDtos.OrderSummaryResponse(
                        rs.getString("order_id"),
                        rs.getString("customer_id"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status"),
                        rs.getString("payment_status"),
                        rs.getString("created_at")
                ));
    }
}
