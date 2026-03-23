package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Repository
public class PaymentStatusRepository {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public PaymentStatusRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.PaymentStatusUpdateResponse updateOrderPaymentStatus(String orderId, String status, String gatewayTxnId) {
        try {
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
        } catch (DataAccessException ex) {
            if (!isPaymentMethodTruncation(ex)) {
                throw ex;
            }
            logger.warn("sp_update_order_payment_status failed due to payment method mismatch. Applying fallback for orderId={}", orderId, ex);
            applyMethodSafeFallback(orderId, status, gatewayTxnId);
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

    private boolean isPaymentMethodTruncation(DataAccessException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("data truncated") && normalized.contains("column 'method'");
    }

    private void applyMethodSafeFallback(String orderId, String status, String gatewayTxnId) {
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE id = ?",
                Integer.class,
                orderId
        );
        if (orderCount == null || orderCount == 0) {
            throw new java.util.NoSuchElementException("Order not found for payment status update");
        }

        String mappedPaymentStatus = mapPaymentStatusForSchema(status);
        jdbcTemplate.update("UPDATE orders SET payment_status = ? WHERE id = ?", mappedPaymentStatus, orderId);

        int updated = jdbcTemplate.update(
                "UPDATE payment SET status = ?, gateway_txn_id = COALESCE(?, gateway_txn_id) WHERE order_id = ?",
                mappedPaymentStatus,
                gatewayTxnId,
                orderId
        );

        if (updated > 0) {
            return;
        }

        String safeMethod = resolveSafePaymentMethod(orderId);
        BigDecimal amount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(total_amount, 0) FROM orders WHERE id = ?",
                BigDecimal.class,
                orderId
        );

        jdbcTemplate.update(
                "INSERT INTO payment(id, order_id, method, gateway_txn_id, amount, status, meta, created_at) " +
                        "VALUES (UUID(), ?, ?, ?, ?, ?, JSON_OBJECT('source', 'payments/update-status-fallback'), CURRENT_TIMESTAMP)",
                orderId,
                safeMethod,
                gatewayTxnId,
                amount,
                mappedPaymentStatus
        );
    }

    private String mapPaymentStatusForSchema(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String mapped = switch (normalized) {
            case "SUCCESS", "PAID" -> "paid";
            case "FAILED" -> "failed";
            case "PENDING" -> "pending";
            default -> normalized.isBlank() ? "pending" : normalized.toLowerCase(Locale.ROOT);
        };

        boolean ordersSupportsPaid = columnSupportsEnumValue("orders", "payment_status", "paid");
        boolean paymentSupportsPaid = columnSupportsEnumValue("payment", "status", "paid");
        if ("paid".equals(mapped) && (!ordersSupportsPaid || !paymentSupportsPaid)) {
            return "success";
        }
        return mapped;
    }

    private String resolveSafePaymentMethod(String orderId) {
        String requestedMethod = jdbcTemplate.queryForObject(
                "SELECT COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(meta, '$.paymentMethod')), ''), '') FROM orders WHERE id = ?",
                String.class,
                orderId
        );

        if (requestedMethod != null && !requestedMethod.isBlank() && columnSupportsEnumValue("payment", "method", requestedMethod)) {
            return requestedMethod;
        }

        String fallback = firstEnumValue("payment", "method");
        if (fallback == null || fallback.isBlank()) {
            return "UPI";
        }
        return fallback;
    }

    private boolean columnSupportsEnumValue(String tableName, String columnName, String enumValue) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? " +
                "AND LOCATE(CONCAT(CHAR(39), LOWER(?), CHAR(39)), LOWER(COLUMN_TYPE)) > 0",
                Integer.class,
                tableName,
                columnName,
                enumValue
        );
        return count != null && count > 0;
    }

    private String firstEnumValue(String tableName, String columnName) {
        String columnType = jdbcTemplate.queryForObject(
                "SELECT COLUMN_TYPE FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                String.class,
                tableName,
                columnName
        );

        if (columnType == null || !columnType.toLowerCase(Locale.ROOT).startsWith("enum(")) {
            return null;
        }

        int firstQuote = columnType.indexOf('\'');
        if (firstQuote < 0) {
            return null;
        }
        int secondQuote = columnType.indexOf('\'', firstQuote + 1);
        if (secondQuote <= firstQuote + 1) {
            return null;
        }
        return columnType.substring(firstQuote + 1, secondQuote);
    }

    private void synchronizePaidState(String orderId) {
        try {
            jdbcTemplate.update(
                "UPDATE orders SET status = 'paid' WHERE id = ?",
                    orderId
            );
            jdbcTemplate.update(
                "UPDATE payment p JOIN orders o ON o.id = p.order_id " +
                    "SET p.status = o.payment_status WHERE p.order_id = ?",
                    orderId
            );
        } catch (DataAccessException ex) {
            logger.warn("Paid-state synchronization failed for orderId={}", orderId, ex);
        }
    }
}
