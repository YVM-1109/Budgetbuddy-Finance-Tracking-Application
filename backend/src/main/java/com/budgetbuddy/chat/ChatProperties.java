package com.budgetbuddy.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAI chat configuration. The API key lives only in server environment
 * configuration and is never sent to the browser (PRD 13).
 */
@ConfigurationProperties(prefix = "budgetbuddy.chat")
public record ChatProperties(
        String openaiApiKey,
        String model,
        int timeoutSeconds,
        int maxInputLength
) {
}
