package com.budgetbuddy.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Transaction representation returned to clients.
 */
public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        LocalDate transactionDate,
        String category,
        String remarks,
        TransactionSource sourceType,
        UUID recurringFinanceId,
        Instant createdAt,
        Instant updatedAt
) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getType(),
                t.getAmount(),
                t.getTransactionDate(),
                t.getCategory(),
                t.getRemarks(),
                t.getSourceType(),
                t.getRecurringFinance() != null ? t.getRecurringFinance().getId() : null,
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
