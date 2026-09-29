package com.budgetbuddy.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-wide operational properties.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String timezone, String baseUrl) {
}
