package com.buyglimmer.backend.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class PasswordResetTokenRepository {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public PasswordResetTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createPasswordResetToken(String email, String resetToken, long expiresAtEpochSeconds) {
        try {
            String sql = "CALL sp_create_password_reset_token(?, ?, ?)";
            jdbcTemplate.update(sql, email, resetToken, expiresAtEpochSeconds);
            logger.info("Password reset token created for email={}", email);
        } catch (DataAccessException e) {
            logger.error("Failed to create password reset token for email={}", email, e);
            throw e;
        }
    }

    public Optional<ResetTokenInfo> getPasswordResetToken(String resetToken) {
        try {
            String sql = "CALL sp_get_password_reset_token(?)";
            return jdbcTemplate.query(sql, new Object[]{resetToken}, resultSet -> {
                if (resultSet.next()) {
                    return Optional.of(new ResetTokenInfo(
                            resultSet.getString("email"),
                            resultSet.getString("reset_token"),
                            resultSet.getLong("expires_at_epoch_seconds"),
                            resultSet.getBoolean("is_used"),
                            resultSet.getString("created_at")
                    ));
                }
                return Optional.empty();
            });
        } catch (DataAccessException e) {
            logger.error("Failed to retrieve password reset token={}", resetToken, e);
            return Optional.empty();
        }
    }

    public int markPasswordResetTokenUsed(String resetToken) {
        try {
            String sql = "CALL sp_mark_password_reset_token_used(?)";
            jdbcTemplate.update(sql, resetToken);
            logger.info("Password reset token marked as used token={}", resetToken);
            return 1;
        } catch (DataAccessException e) {
            logger.error("Failed to mark password reset token as used token={}", resetToken, e);
            throw e;
        }
    }

    public int cleanupExpiredTokens() {
        try {
            String sql = "CALL sp_cleanup_expired_password_reset_tokens()";
            return jdbcTemplate.update(sql);
        } catch (DataAccessException e) {
            logger.error("Failed to cleanup expired password reset tokens", e);
            throw e;
        }
    }

    public record ResetTokenInfo(
            String email,
            String resetToken,
            long expiresAtEpochSeconds,
            boolean isUsed,
            String createdAt
    ) {
    }
}
