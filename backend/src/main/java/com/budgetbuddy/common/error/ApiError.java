package com.budgetbuddy.common.error;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent JSON error shape returned for every failure
 * (per TECHNICAL_SPEC section 13).
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
