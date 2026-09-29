package com.budgetbuddy.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT signing configuration. The secret must be provided via JWT_SECRET
 * in every real environment; the default exists only for local development.
 */
@ConfigurationProperties(prefix = "budgetbuddy.jwt")
public record JwtProperties(String secret, long expirationMinutes) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is required (set JWT_SECRET).");
        }
        if (secret.getBytes().length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes.");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("JWT expiration must be positive.");
        }
    }
}
