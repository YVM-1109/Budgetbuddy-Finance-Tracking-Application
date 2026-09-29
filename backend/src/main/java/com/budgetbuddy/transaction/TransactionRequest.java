package com.budgetbuddy.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create/update transaction request (API_SPEC). Past, current and future
 * dates are allowed (DEC-010); amounts must be positive with at most two
 * decimal places (PRD 7.3 / TECHNICAL_SPEC 8).
 */
public record TransactionRequest(
        @NotNull(message = "Type is required.")
        TransactionType type,

        @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
        @Digits(integer = 13, fraction = 2, message = "Amount supports at most two decimal places.")
        BigDecimal amount,

        @NotNull(message = "Transaction date is required.")
        LocalDate transactionDate,

        @NotBlank(message = "Category is required.")
        @Size(max = 80, message = "Category must be at most 80 characters.")
        String category,

        @Size(max = 500, message = "Remarks must be at most 500 characters.")
        String remarks
) {
}
