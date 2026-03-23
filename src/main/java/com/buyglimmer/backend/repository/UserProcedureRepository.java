package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserProcedureRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.UserProfileResponse getProfile(String customerId) {
    List<FintechDtos.UserProfileResponse> rows = jdbcTemplate.query("CALL sp_get_profile(?)",
        ps -> ps.setString(1, customerId),
        (rs, rowNum) -> new FintechDtos.UserProfileResponse(
                        rs.getString("customer_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("mobile"),
                        rs.getInt("status"),
            rs.getString("created_at")
                ));
    if (rows.isEmpty()) {
        throw new java.util.NoSuchElementException("User profile not found");
    }
    return rows.get(0);
    }

    public FintechDtos.UserProfileResponse updateProfile(FintechDtos.UserUpdateRequest request) {
    List<FintechDtos.UserProfileResponse> rows = jdbcTemplate.query("CALL sp_update_profile(?, ?, ?, ?)",
        ps -> {
            ps.setString(1, request.customerId());
            ps.setString(2, request.name());
            ps.setString(3, request.email());
            ps.setString(4, request.mobile());
        },
        (rs, rowNum) -> new FintechDtos.UserProfileResponse(
            rs.getString("customer_id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("mobile"),
            rs.getInt("status"),
            rs.getString("created_at")
        ));
    if (rows.isEmpty()) {
        throw new java.util.NoSuchElementException("User profile not found after update");
    }
    return rows.get(0);
    }

    public FintechDtos.AddressResponse addAddress(FintechDtos.AddressAddRequest request) {
        List<FintechDtos.AddressResponse> rows = jdbcTemplate.query("CALL sp_add_address(?, ?, ?, ?, ?, ?, ?)",
                ps -> {
                    ps.setString(1, request.customerId());
                    ps.setString(2, request.type());
                    ps.setString(3, request.addressLine());
                    ps.setString(4, request.city());
                    ps.setString(5, request.state());
                    ps.setString(6, request.pincode());
                    ps.setBoolean(7, Boolean.TRUE.equals(request.isDefault()));
                },
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

    public List<FintechDtos.AddressResponse> listAddresses(String customerId) {
        return jdbcTemplate.query("CALL sp_get_addresses(?)",
                ps -> ps.setString(1, customerId),
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
    }
}
