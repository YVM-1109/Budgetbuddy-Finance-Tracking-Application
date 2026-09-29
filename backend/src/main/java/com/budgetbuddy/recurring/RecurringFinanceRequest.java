package com.budgetbuddy.recurring;

import com.budgetbuddy.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create/update recurring finance request (API_SPEC).
 */
public record RecurringFinanceRequest(
        @NotNull(message = "Type is required.")
        TransactionType type,

        @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
        @Digits(integer = 13, fraction = 2, message = "Amount supports at most two decimal places.")
        BigDecimal amount,

        @NotBlank(message = "Category is required.")
        @Size(max = 80, message = "Category must be at most 80 characters.")
        String category,

        @NotNull(message = "Start date is required.")
        LocalDate startDate,

        LocalDate endDate,

        @Size(max = 500, message = "Remarks must be at most 500 characters.")
        String remarks,

        Boolean active
) {
}
