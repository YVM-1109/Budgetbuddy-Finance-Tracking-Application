package com.budgetbuddy.recurring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RecurringGenerationRecordRepository extends JpaRepository<RecurringGenerationRecord, UUID> {

    Optional<RecurringGenerationRecord> findTopByRecurringFinanceIdOrderByPeriodYearDescPeriodMonthDesc(
            UUID recurringFinanceId);

    boolean existsByRecurringFinanceIdAndPeriodYearAndPeriodMonth(UUID recurringFinanceId, int year, int month);

    Optional<RecurringGenerationRecord> findByRecurringFinanceIdAndPeriodYearAndPeriodMonth(
            UUID recurringFinanceId, int year, int month);
}
