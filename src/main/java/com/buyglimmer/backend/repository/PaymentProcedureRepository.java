package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class PaymentProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentProcedureRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.PaymentResponse createPayment(FintechDtos.PaymentCreateRequest request) {
    String paymentId = UUID.randomUUID().toString();
    jdbcTemplate.update("""
            INSERT INTO payment(id, order_id, method, gateway_txn_id, amount, status, meta, created_at)
            VALUES (?, ?, ?, ?, ?, 'pending', NULL, CURRENT_TIMESTAMP)
            """,
        paymentId,
        request.orderId(),
        request.method(),
        request.gatewayTxnId(),
        request.amount());

    jdbcTemplate.update("UPDATE orders SET payment_status = 'pending' WHERE id = ?", request.orderId());
    return paymentById(paymentId);
    }

    public FintechDtos.PaymentResponse verifyPayment(FintechDtos.PaymentVerifyRequest request) {
    jdbcTemplate.update("""
            UPDATE payment
            SET gateway_txn_id = ?, status = ?
            WHERE id = ?
            """,
        request.gatewayTxnId(), request.status().toLowerCase(), request.paymentId());

    jdbcTemplate.update("""
            UPDATE orders
            SET payment_status = ?
            WHERE id = (SELECT order_id FROM payment WHERE id = ?)
            """,
        request.status().toLowerCase(), request.paymentId());

    return paymentById(request.paymentId());
    }

    private FintechDtos.PaymentResponse paymentById(String paymentId) {
    List<FintechDtos.PaymentResponse> rows = jdbcTemplate.query("""
            SELECT id AS payment_id,
                   order_id,
                   method,
                   gateway_txn_id,
                   amount,
                   status
            FROM payment
            WHERE id = ?
            """,
        ps -> ps.setString(1, paymentId),
        (rs, rowNum) -> new FintechDtos.PaymentResponse(
            rs.getString("payment_id"),
            rs.getString("order_id"),
            rs.getString("method"),
            rs.getString("gateway_txn_id"),
            rs.getBigDecimal("amount"),
            rs.getString("status")
        ));
    if (rows.isEmpty()) {
        throw new java.util.NoSuchElementException("Payment not found");
    }
    return rows.get(0);
    }
}
