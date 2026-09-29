package com.budgetbuddy.recurring;

import com.budgetbuddy.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Recurring finance representation returned to clients.
 */
public record RecurringFinanceResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        String category,
        String remarks,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static RecurringFinanceResponse from(RecurringFinance rf) {
        return new RecurringFinanceResponse(
                rf.getId(),
                rf.getType(),
                rf.getAmount(),
                rf.getCategory(),
                rf.getRemarks(),
                rf.getStartDate(),
                rf.getEndDate(),
                rf.isActive(),
                rf.getCreatedAt(),
                rf.getUpdatedAt()
        );
    }
}
