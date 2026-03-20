package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CouponProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public CouponProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.CouponValidationResponse validateCoupon(FintechDtos.CouponValidateRequest request) {
        List<FintechDtos.CouponValidationResponse> rows = jdbcTemplate.query(
                "CALL sp_validate_coupon(?, ?, ?)",
                ps -> {
                    ps.setString(1, request.customerId());
                    ps.setString(2, request.couponCode());
                    ps.setBigDecimal(3, request.orderAmount());
                },
                (rs, rowNum) -> new FintechDtos.CouponValidationResponse(
                        rs.getBoolean("is_valid"),
                        rs.getBigDecimal("discount_amount"),
                        rs.getString("message")
                ));
        if (rows.isEmpty()) {
            return new FintechDtos.CouponValidationResponse(false, java.math.BigDecimal.ZERO, "Invalid coupon");
        }
        return rows.get(0);
    }
}
