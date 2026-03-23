package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PaymentStatusRepository {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusRepository.class);

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

        String normalized = status == null ? "" : status.trim().toUpperCase();
        if ("SUCCESS".equals(normalized) || "PAID".equals(normalized)) {
            synchronizePaidState(orderId);
        }

        String paymentStatus = jdbcTemplate.queryForObject(
                "SELECT payment_status FROM orders WHERE id = ?",
                String.class,
                orderId
        );

        return new FintechDtos.PaymentStatusUpdateResponse(orderId, paymentStatus);
    }

    private void synchronizePaidState(String orderId) {
        try {
            jdbcTemplate.update(
                    "UPDATE orders SET status = 'paid', payment_status = 'paid' WHERE id = ?",
                    orderId
            );
            jdbcTemplate.update(
                    "UPDATE payment SET status = 'paid' WHERE order_id = ?",
                    orderId
            );
        } catch (DataAccessException ex) {
            logger.warn("Paid-state synchronization failed for orderId={}. Falling back to success status.", orderId, ex);
            jdbcTemplate.update(
                    "UPDATE orders SET status = 'success', payment_status = 'success' WHERE id = ?",
                    orderId
            );
            jdbcTemplate.update(
                    "UPDATE payment SET status = 'success' WHERE order_id = ?",
                    orderId
            );
        }
    }
}
