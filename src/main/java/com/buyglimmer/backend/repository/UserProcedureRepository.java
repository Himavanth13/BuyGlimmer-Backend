package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class UserProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserProcedureRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.UserProfileResponse getProfile(String customerId) {
    List<FintechDtos.UserProfileResponse> rows = jdbcTemplate.query("""
            SELECT id AS customer_id,
                   name,
                   email,
                   mobile,
                   status,
                   created_at
            FROM customer
            WHERE id = ?
            """,
        ps -> ps.setString(1, customerId),
        (rs, rowNum) -> new FintechDtos.UserProfileResponse(
                        rs.getString("customer_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("mobile"),
                        rs.getInt("status"),
            toStringSafe(rs.getTimestamp("created_at"))
                ));
    if (rows.isEmpty()) {
        throw new java.util.NoSuchElementException("User profile not found");
    }
    return rows.get(0);
    }

    public FintechDtos.UserProfileResponse updateProfile(FintechDtos.UserUpdateRequest request) {
    jdbcTemplate.update("""
            UPDATE customer
            SET name = ?, email = ?, mobile = ?
            WHERE id = ?
            """,
        request.name(), request.email(), request.mobile(), request.customerId());
    return getProfile(request.customerId());
    }

    public FintechDtos.AddressResponse addAddress(FintechDtos.AddressAddRequest request) {
        if (Boolean.TRUE.equals(request.isDefault())) {
            jdbcTemplate.update("UPDATE address SET is_default = FALSE WHERE customer_id = ?", request.customerId());
        }

        String addressId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                        INSERT INTO address(id, customer_id, type, address_line, city, state, pincode, is_default)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                addressId,
                request.customerId(),
                request.type(),
                request.addressLine(),
                request.city(),
                request.state(),
                request.pincode(),
                Boolean.TRUE.equals(request.isDefault()));

        List<FintechDtos.AddressResponse> rows = jdbcTemplate.query("""
                        SELECT id AS address_id,
                               customer_id,
                               type,
                               address_line,
                               city,
                               state,
                               pincode,
                               is_default
                        FROM address
                        WHERE id = ?
                        """,
                ps -> ps.setString(1, addressId),
                (rs, rowNum) -> new FintechDtos.AddressResponse(
                        rs.getString("address_id"),
                        rs.getString("customer_id"),
                        rs.getString("type"),
                        rs.getString("address_line"),
                        rs.getString("city"),
                        rs.getString("state"),
                        rs.getString("pincode"),
                        rs.getBoolean("is_default")
                ));
        if (rows.isEmpty()) {
            throw new java.util.NoSuchElementException("Address not found after insert");
        }
        return rows.get(0);
    }

    private String toStringSafe(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toString();
    }
}
