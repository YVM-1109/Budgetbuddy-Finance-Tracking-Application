package com.budgetbuddy.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS allowed origins, configurable per environment.
 */
@ConfigurationProperties(prefix = "budgetbuddy.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
