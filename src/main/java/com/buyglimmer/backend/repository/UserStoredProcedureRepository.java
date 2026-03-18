package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.UserDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserStoredProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserStoredProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserDtos.UserProfileResponse fetchProfile(String userId) {
        List<UserDtos.UserProfileResponse> profiles = jdbcTemplate.query("""
                select id, name, email, mobile as phone
                from customer
                where id = ?
                """,
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
        List<StoredUser> users = jdbcTemplate.query("""
                select id, name, email, password_hash as password, mobile as phone
                from customer
                where email = ?
                """,
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
        String userId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into customer (id, name, email, mobile, password_hash, status, created_at)
                values (?, ?, ?, ?, ?, 1, current_timestamp)
                """,
            userId, name, email, phone, password);
        return fetchProfile(userId);
    }

    public UserDtos.UserProfileResponse updateProfile(String userId, UserDtos.UpdateProfileRequest request) {
        int rows = jdbcTemplate.update("""
                update customer
                set name = ?, email = ?, mobile = ?
                where id = ?
                """,
            request.name(), request.email(), request.phone(), userId);
        if (rows == 0) {
            throw new NoSuchElementException("User not found");
        }
        return fetchProfile(userId);
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