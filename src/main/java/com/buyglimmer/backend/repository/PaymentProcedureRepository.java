package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PaymentProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentProcedureRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.PaymentResponse createPayment(FintechDtos.PaymentCreateRequest request) {
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
    }

    public FintechDtos.PaymentResponse verifyPayment(FintechDtos.PaymentVerifyRequest request) {
    List<FintechDtos.PaymentResponse> rows = jdbcTemplate.query("CALL sp_verify_payment(?, ?, ?)",
        ps -> {
            ps.setString(1, request.paymentId());
            ps.setString(2, request.gatewayTxnId());
            ps.setString(3, request.status());
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
    }
}
