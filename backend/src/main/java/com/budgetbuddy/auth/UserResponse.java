package com.budgetbuddy.auth;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a user; never includes the password hash.
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        String authProvider,
        BigDecimal monthlyBudget,
        BigDecimal monthlySavingsTarget,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAuthProvider().name(),
                user.getMonthlyBudget(),
                user.getMonthlySavingsTarget(),
                user.getCreatedAt()
        );
    }
}
