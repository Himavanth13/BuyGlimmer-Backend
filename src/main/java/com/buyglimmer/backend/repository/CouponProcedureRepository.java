package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class CouponProcedureRepository {

    private static final Logger logger = LoggerFactory.getLogger(CouponProcedureRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public CouponProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.CouponValidationResponse validateCoupon(FintechDtos.CouponValidateRequest request) {
        List<FintechDtos.CouponValidationResponse> rows;
        try {
            rows = jdbcTemplate.query(
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
        } catch (DataAccessException ex) {
            logger.warn("sp_validate_coupon failed; using SQL fallback for couponCode={}", request.couponCode(), ex);
            return validateCouponFallback(request);
        }

        if (rows.isEmpty()) {
            return new FintechDtos.CouponValidationResponse(false, java.math.BigDecimal.ZERO, "Invalid coupon");
        }
        return rows.get(0);
    }

    private FintechDtos.CouponValidationResponse validateCouponFallback(FintechDtos.CouponValidateRequest request) {
        return jdbcTemplate.query(
                "SELECT * FROM coupon WHERE code = ? LIMIT 1",
                ps -> ps.setString(1, request.couponCode()),
                rs -> {
                    if (!rs.next()) {
                        return new FintechDtos.CouponValidationResponse(false, BigDecimal.ZERO, "Coupon not found");
                    }

                    Set<String> columns = columnLabels(rs.getMetaData());
                    String discountType = columns.contains("discount_type") ? rs.getString("discount_type") : "amount";
                    BigDecimal discountValue = columns.contains("discount_value") && rs.getBigDecimal("discount_value") != null
                            ? rs.getBigDecimal("discount_value")
                            : BigDecimal.ZERO;
                    BigDecimal minOrderAmount = columns.contains("min_order_amount") && rs.getBigDecimal("min_order_amount") != null
                            ? rs.getBigDecimal("min_order_amount")
                            : BigDecimal.ZERO;
                    boolean active = !columns.contains("active") || rs.getBoolean("active");

                    return evaluateCoupon(discountType, discountValue, minOrderAmount, active, request.orderAmount());
                }
        );
    }

    private Set<String> columnLabels(ResultSetMetaData metaData) throws SQLException {
        Set<String> labels = new HashSet<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            labels.add(metaData.getColumnLabel(i).toLowerCase());
        }
        return labels;
    }

    private FintechDtos.CouponValidationResponse evaluateCoupon(
            String discountType,
            BigDecimal discountValue,
            BigDecimal minOrderAmount,
            boolean active,
            BigDecimal orderAmount
    ) {
        if (!active) {
            return new FintechDtos.CouponValidationResponse(false, BigDecimal.ZERO, "Coupon is inactive");
        }
        if (orderAmount.compareTo(minOrderAmount) < 0) {
            return new FintechDtos.CouponValidationResponse(false, BigDecimal.ZERO, "Minimum order amount not met");
        }

        BigDecimal discountAmount;
        if ("percent".equalsIgnoreCase(discountType)) {
            discountAmount = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100));
        } else {
            discountAmount = discountValue;
        }

        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }

        return new FintechDtos.CouponValidationResponse(true, discountAmount, "Coupon applied successfully");
    }
}
