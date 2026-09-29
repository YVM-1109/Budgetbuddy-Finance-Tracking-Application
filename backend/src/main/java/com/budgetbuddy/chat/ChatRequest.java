package com.budgetbuddy.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Chat request (API_SPEC).
 */
public record ChatRequest(
        @NotBlank(message = "Message is required.")
        @Size(max = 2000, message = "Message must be at most 2000 characters.")
        String message
) {
}
