package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Repository
public class CouponProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public CouponProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.CouponValidationResponse validateCoupon(FintechDtos.CouponValidateRequest request) {
        try {
            Set<String> columns = loadCouponColumns();

            String codeColumn = pickFirst(columns, "code", "coupon_code", "promo_code");
            if (codeColumn == null) {
                return new FintechDtos.CouponValidationResponse(false, BigDecimal.ZERO, "Invalid coupon");
            }

            String discountTypeColumn = pickFirst(columns, "discount_type", "type");
            String discountValueColumn = pickFirst(columns, "discount_value", "value", "amount");
            String activeColumn = pickFirst(columns, "active", "is_active");
            String minOrderColumn = pickFirst(columns, "min_order_amount", "minimum_order_amount", "min_amount", "minimum_amount");

            String discountTypeExpr = discountTypeColumn != null ? discountTypeColumn : "'FLAT'";
            String discountValueExpr = discountValueColumn != null ? discountValueColumn : "0";
            String activeExpr = activeColumn != null ? activeColumn : "1";
            String minOrderExpr = minOrderColumn != null ? minOrderColumn : "0";

            String sql = """
                    SELECT %s AS discount_type,
                           %s AS discount_value,
                           %s AS active,
                           %s AS min_order_amount
                    FROM coupon
                    WHERE LOWER(%s) = LOWER(?)
                    LIMIT 1
                    """.formatted(discountTypeExpr, discountValueExpr, activeExpr, minOrderExpr, codeColumn);

            List<FintechDtos.CouponValidationResponse> rows = jdbcTemplate.query(
                    sql,
                    ps -> ps.setString(1, request.couponCode()),
                    (rs, rowNum) -> mapCouponValidation(rs, request.orderAmount()));

            if (rows.isEmpty()) {
                return new FintechDtos.CouponValidationResponse(false, BigDecimal.ZERO, "Invalid coupon");
            }
            return rows.get(0);
        } catch (DataAccessException ignored) {
            return new FintechDtos.CouponValidationResponse(false, BigDecimal.ZERO, "Invalid coupon");
        }
    }

    private Set<String> loadCouponColumns() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SHOW COLUMNS FROM coupon");
        Set<String> columns = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object field = row.get("Field");
            if (field != null) {
                columns.add(field.toString().toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private String pickFirst(Set<String> columns, String... candidates) {
        for (String candidate : candidates) {
            if (columns.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return null;
    }

    private FintechDtos.CouponValidationResponse mapCouponValidation(ResultSet rs, BigDecimal orderAmount) throws SQLException {
        if (!toBoolean(rs.getObject("active"))) {
            return new FintechDtos.CouponValidationResponse(false, BigDecimal.ZERO, "Coupon is inactive");
        }

        BigDecimal minOrderAmount = toBigDecimal(rs.getObject("min_order_amount"));
        if (orderAmount.compareTo(minOrderAmount) < 0) {
            return new FintechDtos.CouponValidationResponse(false, BigDecimal.ZERO,
                    "Minimum order amount is " + minOrderAmount.stripTrailingZeros().toPlainString());
        }

        BigDecimal discountValue = toBigDecimal(rs.getObject("discount_value"));
        String discountType = rs.getString("discount_type");

        BigDecimal discount;
        if (discountType != null && "PERCENT".equalsIgnoreCase(discountType)) {
            discount = orderAmount
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = discountValue;
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }

        return new FintechDtos.CouponValidationResponse(true, discount, "Coupon applied successfully");
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = value.toString().trim().toLowerCase(Locale.ROOT);
        return "1".equals(text) || "true".equals(text) || "y".equals(text) || "yes".equals(text) || "active".equals(text);
    }
}
