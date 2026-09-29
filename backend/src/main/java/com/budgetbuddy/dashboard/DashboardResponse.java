package com.budgetbuddy.dashboard;

import com.budgetbuddy.transaction.TransactionResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard DTO (API_SPEC): display-ready aggregates.
 */
public record DashboardResponse(
        MonthTotals currentMonth,
        MonthTotals last12Months,
        BigDecimal budget,
        BigDecimal savingsTarget,
        BudgetInfo budgetInfo,
        SavingsTargetInfo savingsTargetInfo,
        List<MonthlyPoint> monthlySeries,
        List<CategoryShare> categoryBreakdown,
        List<TransactionResponse> recentTransactions
) {

    public record MonthTotals(BigDecimal income, BigDecimal expense, BigDecimal net) {
    }

    public record BudgetInfo(
            BigDecimal budget,
            BigDecimal currentMonthExpense,
            boolean overBudget,
            BigDecimal exceededBy,
            BigDecimal utilizationPercent
    ) {
    }

    public record SavingsTargetInfo(
            BigDecimal target,
            BigDecimal currentMonthNet,
            BigDecimal progressPercent
    ) {
    }

    public record MonthlyPoint(
            int year,
            int month,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net
    ) {
    }

    public record CategoryShare(String category, BigDecimal total) {
    }
}
