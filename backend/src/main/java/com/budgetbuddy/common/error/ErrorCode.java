package com.budgetbuddy.common.error;

/**
 * Machine-readable error codes used in the JSON error contract.
 */
public enum ErrorCode {
    VALIDATION_ERROR,
    INVALID_CREDENTIALS,
    EMAIL_EXISTS,
    NOT_FOUND,
    FORBIDDEN,
    UNAUTHORIZED,
    RATE_LIMITED,
    CHAT_UPSTREAM_ERROR,
    CHAT_TIMEOUT,
    CONFLICT
}
