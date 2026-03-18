package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.util.DbCallUtils;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentProcedureRepository {

    private final DbCallUtils dbCallUtils;

    public PaymentProcedureRepository(DbCallUtils dbCallUtils) {
        this.dbCallUtils = dbCallUtils;
    }

    public FintechDtos.PaymentResponse createPayment(FintechDtos.PaymentCreateRequest request) {
        return dbCallUtils.callForObject("{call sp_create_payment(?,?,?,?)}",
                cs -> {
                    cs.setString(1, request.orderId());
                    cs.setString(2, request.method());
                    cs.setString(3, request.gatewayTxnId());
                    cs.setBigDecimal(4, request.amount());
                },
                rs -> new FintechDtos.PaymentResponse(
                        rs.getString("payment_id"),
                        rs.getString("order_id"),
                        rs.getString("method"),
                        rs.getString("gateway_txn_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("status")
                ));
    }

    public FintechDtos.PaymentResponse verifyPayment(FintechDtos.PaymentVerifyRequest request) {
        return dbCallUtils.callForObject("{call sp_verify_payment(?,?,?)}",
                cs -> {
                    cs.setString(1, request.paymentId());
                    cs.setString(2, request.gatewayTxnId());
                    cs.setString(3, request.status());
                },
                rs -> new FintechDtos.PaymentResponse(
                        rs.getString("payment_id"),
                        rs.getString("order_id"),
                        rs.getString("method"),
                        rs.getString("gateway_txn_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("status")
                ));
    }
}
