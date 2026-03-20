package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Repository
public class PaymentProcedureRepository {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcedureRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public PaymentProcedureRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.PaymentResponse createPayment(FintechDtos.PaymentCreateRequest request) {
    try {
        List<FintechDtos.PaymentResponse> rows = jdbcTemplate.query("CALL sp_create_payment(?, ?, ?, ?)",
            ps -> {
                ps.setString(1, request.orderId());
                ps.setString(2, request.method());
                ps.setString(3, request.gatewayTxnId());
                ps.setBigDecimal(4, request.amount());
            },
            (rs, rowNum) -> new FintechDtos.PaymentResponse(
                rs.getString("payment_id"),
                rs.getString("order_id"),
                rs.getString("method"),
                rs.getString("gateway_txn_id"),
                rs.getBigDecimal("amount"),
                rs.getString("status")
            ));
        if (rows.isEmpty()) {
            throw new java.util.NoSuchElementException("Payment not created");
        }
        return rows.get(0);
    } catch (DataAccessException ex) {
        logger.warn("sp_create_payment failed; using SQL fallback. orderId={}", request.orderId(), ex);
        String paymentId = UUID.randomUUID().toString();
        jdbcTemplate.update(
            "INSERT INTO payment(id, order_id, method, gateway_txn_id, amount, status, meta, created_at) VALUES (?, ?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP)",
            paymentId,
            request.orderId(),
            request.method(),
            request.gatewayTxnId(),
            request.amount(),
            "pending"
        );
        jdbcTemplate.update("UPDATE orders SET payment_status = ? WHERE id = ?", "pending", request.orderId());
        return jdbcTemplate.queryForObject(
            "SELECT id AS payment_id, order_id, method, gateway_txn_id, amount, status FROM payment WHERE id = ?",
            (rs, rowNum) -> new FintechDtos.PaymentResponse(
                rs.getString("payment_id"),
                rs.getString("order_id"),
                rs.getString("method"),
                rs.getString("gateway_txn_id"),
                rs.getBigDecimal("amount"),
                rs.getString("status")
            ),
            paymentId
        );
    }
    }

    public FintechDtos.PaymentResponse verifyPayment(FintechDtos.PaymentVerifyRequest request) {
    String normalizedStatus = request.status() == null ? null : request.status().toLowerCase(Locale.ROOT);
    try {
        List<FintechDtos.PaymentResponse> rows = jdbcTemplate.query("CALL sp_verify_payment(?, ?, ?)",
            ps -> {
                ps.setString(1, request.paymentId());
                ps.setString(2, request.gatewayTxnId());
                ps.setString(3, normalizedStatus);
            },
            (rs, rowNum) -> new FintechDtos.PaymentResponse(
                rs.getString("payment_id"),
                rs.getString("order_id"),
                rs.getString("method"),
                rs.getString("gateway_txn_id"),
                rs.getBigDecimal("amount"),
                rs.getString("status")
            ));
        if (rows.isEmpty()) {
            throw new java.util.NoSuchElementException("Payment not found after verification");
        }
        return rows.get(0);
    } catch (DataAccessException ex) {
        logger.warn("sp_verify_payment failed; using SQL fallback. paymentId={}", request.paymentId(), ex);
        jdbcTemplate.update(
            "UPDATE payment SET gateway_txn_id = ?, status = ? WHERE id = ?",
            request.gatewayTxnId(),
            normalizedStatus,
            request.paymentId()
        );
        if ("success".equals(normalizedStatus)) {
            jdbcTemplate.update(
                "UPDATE orders SET payment_status = ? WHERE id = (SELECT order_id FROM payment WHERE id = ?)",
                "paid",
                request.paymentId()
            );
        }
        return jdbcTemplate.queryForObject(
            "SELECT id AS payment_id, order_id, method, gateway_txn_id, amount, status FROM payment WHERE id = ?",
            (rs, rowNum) -> new FintechDtos.PaymentResponse(
                rs.getString("payment_id"),
                rs.getString("order_id"),
                rs.getString("method"),
                rs.getString("gateway_txn_id"),
                rs.getBigDecimal("amount"),
                rs.getString("status")
            ),
            request.paymentId()
        );
    }
    }
}
