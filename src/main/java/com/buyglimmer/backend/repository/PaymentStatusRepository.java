package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PaymentStatusRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentStatusRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.PaymentStatusUpdateResponse updateOrderPaymentStatus(String orderId, String status, String gatewayTxnId) {
        List<FintechDtos.PaymentStatusUpdateResponse> rows = jdbcTemplate.query(
                "CALL sp_update_order_payment_status(?, ?, ?)",
                ps -> {
                    ps.setString(1, orderId);
                    ps.setString(2, status);
                    ps.setString(3, gatewayTxnId);
                },
                (rs, rowNum) -> new FintechDtos.PaymentStatusUpdateResponse(
                        rs.getString("order_id"),
                        rs.getString("payment_status")
                )
        );

        if (rows.isEmpty()) {
            throw new java.util.NoSuchElementException("Order not found for payment status update");
        }
        return rows.get(0);
    }
}
