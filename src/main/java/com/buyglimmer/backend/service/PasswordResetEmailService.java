package com.buyglimmer.backend.service;

import com.buyglimmer.backend.BuyGlimmerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final JavaMailSender mailSender;
    private final BuyGlimmerProperties properties;

    public PasswordResetEmailService(JavaMailSender mailSender, BuyGlimmerProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendPasswordResetToken(String email, String resetToken, long expiresInSeconds) {
        try {
            String expiresInMinutes = (expiresInSeconds / 60) + " minutes";
            String resetUrl = String.format(
                    "https://app.buyglimmer.com/reset-password?token=%s&email=%s",
                    resetToken, email
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getEmailFrom());
            message.setTo(email);
            message.setSubject(properties.getEmailForgotPasswordSubject());
            message.setText(buildForgotPasswordEmailBody(resetToken, resetUrl, expiresInMinutes));

            mailSender.send(message);
            logger.info("Password reset token email sent successfully to email={}", email);
        } catch (Exception e) {
            logger.error("Failed to send password reset token email to email={}", email, e);
            // Don't throw exception, just log - UX should not be blocked by email failure
        }
    }

    public void sendPasswordResetSuccessEmail(String email, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getEmailFrom());
            message.setTo(email);
            message.setSubject(properties.getEmailPasswordResetSuccessSubject());
            message.setText(buildPasswordResetSuccessEmailBody(userName));

            mailSender.send(message);
            logger.info("Password reset success email sent to email={}", email);
        } catch (Exception e) {
            logger.error("Failed to send password reset success email to email={}", email, e);
            // Don't throw exception, just log
        }
    }

    private String buildForgotPasswordEmailBody(String resetToken, String resetUrl, String expiresIn) {
        return String.format(
                "Hello,\n\n" +
                "You requested a password reset for your BuyGlimmer account. " +
                "Your reset token is valid for %s.\n\n" +
                "Token: %s\n\n" +
                "Reset Link: %s\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Best regards,\n" +
                "BuyGlimmer Support Team",
                expiresIn, resetToken, resetUrl
        );
    }

    private String buildPasswordResetSuccessEmailBody(String userName) {
        return String.format(
                "Hello %s,\n\n" +
                "Your password has been successfully reset. " +
                "You can now log in with your new password.\n\n" +
                "If this was not you, please contact our support team immediately.\n\n" +
                "Best regards,\n" +
                "BuyGlimmer Support Team",
                userName
        );
    }
}
