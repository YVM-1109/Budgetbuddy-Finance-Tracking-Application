package com.budgetbuddy.auth;

/**
 * Auth response carrying the access token and the user representation.
 */
public record AuthResponse(String token, UserResponse user) {
}
