package com.budgetbuddy.dashboard;

import com.budgetbuddy.auth.User;
import com.budgetbuddy.auth.UserResponse;
import com.budgetbuddy.security.CurrentUserService;
import com.budgetbuddy.transaction.TransactionRepository;
import com.budgetbuddy.transaction.TransactionResponse;
import com.budgetbuddy.transaction.TransactionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Display-ready dashboard aggregates computed in the database
 * (TECHNICAL_SPEC 11); the frontend never recalculates authoritative totals.
 * Reporting period: current calendar month plus the previous 11 (DEC-011),
 * evaluated in the reporting timezone.
 */
@Service
public class DashboardService {

    private static final int RECENT_LIMIT = 5;
    private static final int TOP_CATEGORIES = 5;

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public DashboardService(TransactionRepository transactionRepository,
                            CurrentUserService currentUserService, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse build() {
        UUID userId = currentUserService.currentUserId();
        User user = currentUserService.currentUser();
        YearMonth currentMonth = YearMonth.now(clock);

        // --- Current month ---
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        BigDecimal monthIncome = transactionRepository.sumByTypeBetween(userId, TransactionType.INCOME, monthStart, monthEnd);
        BigDecimal monthExpense = transactionRepository.sumByTypeBetween(userId, TransactionType.EXPENSE, monthStart, monthEnd);
        BigDecimal monthNet = monthIncome.subtract(monthExpense);

        // --- 12 calendar months including current (DEC-011) ---
        YearMonth startMonth = currentMonth.minusMonths(11);
        LocalDate periodStart = startMonth.atDay(1);
        LocalDate periodEnd = currentMonth.atEndOfMonth();
        BigDecimal periodIncome = transactionRepository.sumByTypeBetween(userId, TransactionType.INCOME, periodStart, periodEnd);
        BigDecimal periodExpense = transactionRepository.sumByTypeBetween(userId, TransactionType.EXPENSE, periodStart, periodEnd);
        BigDecimal periodNet = periodIncome.subtract(periodExpense);

        List<DashboardResponse.MonthlyPoint> monthlySeries = buildMonthlySeries(userId, startMonth, currentMonth);

        // --- Category aggregation over the 12-month window ---
        List<DashboardResponse.CategoryShare> categories = transactionRepository
                .sumByCategory(userId, TransactionType.EXPENSE, periodStart, periodEnd).stream()
                .map(ct -> new DashboardResponse.CategoryShare(ct.getCategory(), ct.getTotal()))
                .toList();

        List<TransactionResponse> recent = transactionRepository
                .findByUserIdOrderByTransactionDateDescCreatedAtDesc(userId, PageRequest.of(0, RECENT_LIMIT))
                .stream()
                .map(TransactionResponse::from)
                .toList();

        // --- Budget + savings target (PRD 9, 10) ---
        BigDecimal budget = user.getMonthlyBudget();
        BigDecimal savingsTarget = user.getMonthlySavingsTarget();
        boolean overBudget = budget.signum() > 0 && monthExpense.compareTo(budget) > 0;
        BigDecimal overBy = overBudget ? monthExpense.subtract(budget) : BigDecimal.ZERO;
        BigDecimal utilization = budget.signum() > 0
                ? monthExpense.multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP)
                : null;
        BigDecimal savingsProgress = savingsTarget.signum() > 0
                ? monthNet.multiply(BigDecimal.valueOf(100)).divide(savingsTarget, 2, RoundingMode.HALF_UP)
                : null;

        return new DashboardResponse(
                new DashboardResponse.MonthTotals(monthIncome, monthExpense, monthNet),
                new DashboardResponse.MonthTotals(periodIncome, periodExpense, periodNet),
                budget,
                savingsTarget,
                new DashboardResponse.BudgetInfo(budget, monthExpense, overBudget, overBy, utilization),
                new DashboardResponse.SavingsTargetInfo(savingsTarget, monthNet, savingsProgress),
                monthlySeries,
                categories,
                recent
        );
    }

    private List<DashboardResponse.MonthlyPoint> buildMonthlySeries(UUID userId, YearMonth startMonth, YearMonth endMonth) {
        Map<YearMonth, BigDecimal> income = monthlyMap(transactionRepository.sumMonthlyByType(
                userId, TransactionType.INCOME, startMonth.atDay(1), endMonth.atEndOfMonth()));
        Map<YearMonth, BigDecimal> expense = monthlyMap(transactionRepository.sumMonthlyByType(
                userId, TransactionType.EXPENSE, startMonth.atDay(1), endMonth.atEndOfMonth()));
        List<DashboardResponse.MonthlyPoint> series = new ArrayList<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            BigDecimal i = income.getOrDefault(cursor, BigDecimal.ZERO);
            BigDecimal e = expense.getOrDefault(cursor, BigDecimal.ZERO);
            series.add(new DashboardResponse.MonthlyPoint(cursor.getYear(), cursor.getMonthValue(), i, e, i.subtract(e)));
            cursor = cursor.plusMonths(1);
        }
        return series;
    }

    private Map<YearMonth, BigDecimal> monthlyMap(List<TransactionRepository.MonthlyTotal> totals) {
        Map<YearMonth, BigDecimal> map = new HashMap<>();
        for (TransactionRepository.MonthlyTotal row : totals) {
            map.put(YearMonth.of(row.getYear(), row.getMonth()), row.getTotal());
        }
        return map;
    }
}
