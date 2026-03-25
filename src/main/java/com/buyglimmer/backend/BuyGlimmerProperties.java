package com.buyglimmer.backend;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "buyglimmer")
public class BuyGlimmerProperties {

    private String brand = "BuyGlimmer";
    private String currency = "INR";
    private String supportEmail = "support@buyglimmer.com";
    private String tokenPrefix = "bgm";
    private Email email = new Email();

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public String getEmailFrom() {
        return email.from;
    }

    public String getEmailForgotPasswordSubject() {
        return email.forgotPasswordSubject;
    }

    public String getEmailPasswordResetSuccessSubject() {
        return email.passwordResetSuccessSubject;
    }

    public static class Email {
        private String from = "noreply@buyglimmer.com";
        private String forgotPasswordSubject = "Password Reset Request";
        private String passwordResetSuccessSubject = "Password Reset Successful";

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getForgotPasswordSubject() {
            return forgotPasswordSubject;
        }

        public void setForgotPasswordSubject(String forgotPasswordSubject) {
            this.forgotPasswordSubject = forgotPasswordSubject;
        }

        public String getPasswordResetSuccessSubject() {
            return passwordResetSuccessSubject;
        }

        public void setPasswordResetSuccessSubject(String passwordResetSuccessSubject) {
            this.passwordResetSuccessSubject = passwordResetSuccessSubject;
        }
    }
}