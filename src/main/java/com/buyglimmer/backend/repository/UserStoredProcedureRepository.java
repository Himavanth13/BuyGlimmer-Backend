package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.UserDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
public class UserStoredProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserStoredProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserDtos.UserProfileResponse fetchProfile(String userId) {
        List<UserDtos.UserProfileResponse> profiles = jdbcTemplate.query("CALL sp_fetch_user_profile(?)",
                ps -> ps.setString(1, userId),
                (rs, rowNum) -> new UserDtos.UserProfileResponse(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                null,
                null
                ));
        return profiles.stream().findFirst().orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    public Optional<StoredUser> fetchUserByEmail(String email) {
        List<StoredUser> users = jdbcTemplate.query("CALL sp_fetch_user_by_email(?)",
                ps -> ps.setString(1, email),
                (rs, rowNum) -> new StoredUser(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("phone"),
                null,
                null
                ));
        return users.stream().findFirst();
    }

    public UserDtos.UserProfileResponse register(String name, String email, String password, String phone) {
        String userId = jdbcTemplate.queryForObject(
                "CALL sp_register_user(?, ?, ?, ?)",
                String.class,
                name,
                email,
                password,
                phone
        );
        if (userId == null || userId.isBlank()) {
            throw new NoSuchElementException("User not created");
        }
        return fetchProfile(userId);
    }

    public UserDtos.UserProfileResponse updateProfile(String userId, UserDtos.UpdateProfileRequest request) {
        String updatedUserId = jdbcTemplate.queryForObject(
                "CALL sp_update_user_profile(?, ?, ?, ?, ?, ?)",
                String.class,
                userId,
                request.name(),
                request.email(),
                request.phone(),
                request.address(),
                request.avatar()
        );
        if (updatedUserId == null || updatedUserId.isBlank()) {
            throw new NoSuchElementException("User not found");
        }
        return fetchProfile(updatedUserId);
    }

    public int updatePasswordByEmail(String email, String password) {
        return jdbcTemplate.update(
                "UPDATE customer SET password_hash = ? WHERE LOWER(email) = LOWER(?)",
                password,
                email
        );
    }

    public record StoredUser(
            String id,
            String name,
            String email,
            String password,
            String phone,
            String address,
            String avatar
    ) {
    }
}