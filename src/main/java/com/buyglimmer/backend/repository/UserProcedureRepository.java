package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.util.DbCallUtils;
import org.springframework.stereotype.Repository;

@Repository
public class UserProcedureRepository {

    private final DbCallUtils dbCallUtils;

    public UserProcedureRepository(DbCallUtils dbCallUtils) {
        this.dbCallUtils = dbCallUtils;
    }

    public FintechDtos.UserProfileResponse getProfile(String customerId) {
        return dbCallUtils.callForObject("{call sp_get_profile(?)}",
                cs -> cs.setString(1, customerId),
                rs -> new FintechDtos.UserProfileResponse(
                        rs.getString("customer_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("mobile"),
                        rs.getInt("status"),
                        rs.getString("created_at")
                ));
    }

    public FintechDtos.UserProfileResponse updateProfile(FintechDtos.UserUpdateRequest request) {
        return dbCallUtils.callForObject("{call sp_update_profile(?,?,?,?)}",
                cs -> {
                    cs.setString(1, request.customerId());
                    cs.setString(2, request.name());
                    cs.setString(3, request.email());
                    cs.setString(4, request.mobile());
                },
                rs -> new FintechDtos.UserProfileResponse(
                        rs.getString("customer_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("mobile"),
                        rs.getInt("status"),
                        rs.getString("created_at")
                ));
    }

    public FintechDtos.AddressResponse addAddress(FintechDtos.AddressAddRequest request) {
        return dbCallUtils.callForObject("{call sp_add_address(?,?,?,?,?,?,?)}",
                cs -> {
                    cs.setString(1, request.customerId());
                    cs.setString(2, request.type());
                    cs.setString(3, request.addressLine());
                    cs.setString(4, request.city());
                    cs.setString(5, request.state());
                    cs.setString(6, request.pincode());
                    cs.setBoolean(7, Boolean.TRUE.equals(request.isDefault()));
                },
                rs -> new FintechDtos.AddressResponse(
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
