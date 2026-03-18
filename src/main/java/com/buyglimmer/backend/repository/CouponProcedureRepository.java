package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.util.DbCallUtils;
import org.springframework.stereotype.Repository;

@Repository
public class CouponProcedureRepository {

    private final DbCallUtils dbCallUtils;

    public CouponProcedureRepository(DbCallUtils dbCallUtils) {
        this.dbCallUtils = dbCallUtils;
    }

    public FintechDtos.CouponValidationResponse validateCoupon(FintechDtos.CouponValidateRequest request) {
        return dbCallUtils.callForObject("{call sp_validate_coupon(?,?,?)}",
                cs -> {
                    cs.setString(1, request.customerId());
                    cs.setString(2, request.couponCode());
                    cs.setBigDecimal(3, request.orderAmount());
                },
                rs -> new FintechDtos.CouponValidationResponse(
                        rs.getBoolean("is_valid"),
                        rs.getBigDecimal("discount_amount"),
                        rs.getString("message")
                ));
    }
}
