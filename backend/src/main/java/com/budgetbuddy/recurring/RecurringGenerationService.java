package com.budgetbuddy.recurring;

import com.budgetbuddy.transaction.Transaction;
import com.budgetbuddy.transaction.TransactionRepository;
import com.budgetbuddy.transaction.TransactionSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Deterministic, idempotent monthly generation (PRD 8.2-8.4, TECHNICAL_SPEC 10).
 *
 * Idempotency is enforced at two levels:
 * 1. Application check - skip periods that already have a generation record.
 * 2. Database constraint - unique (recurring_finance_id, period_year,
 *    period_month) makes concurrent or repeated scheduler runs safe; the
 *    loser of a race rolls back and the next pass observes the winner's rows.
 *
 * Amount changes affect future periods only (DEC-009): generated
 * transactions are ordinary historical records and are never rewritten.
 */
@Service
public class RecurringGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RecurringGenerationService.class);

    private final RecurringFinanceRepository recurringRepository;
    private final RecurringGenerationRecordRepository generationRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public RecurringGenerationService(RecurringFinanceRepository recurringRepository,
                                      RecurringGenerationRecordRepository generationRepository,
                                      TransactionRepository transactionRepository,
                                      Clock clock) {
        this.recurringRepository = recurringRepository;
        this.generationRepository = generationRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    /**
     * Reconciles all active recurring configurations (scheduler entry point).
     * Each configuration runs in its own transaction so one failure does not
     * affect the others.
     */
    public void generateForAllUsers() {
        List<UUID> configIds = recurringRepository.findAll().stream()
                .map(RecurringFinance::getId)
                .toList();
        int generated = 0;
        for (UUID configId : configIds) {
            try {
                generated += generateForConfig(configId);
            } catch (DataIntegrityViolationException e) {
                // Lost a generation race; the concurrent winner already created the rows.
                log.debug("Generation race lost for config {}: {}", configId, e.getMessage());
            } catch (Exception e) {
                log.error("Recurring generation failed for config {}", configId, e);
            }
        }
        log.info("Recurring generation pass completed for {} configs, {} transactions created",
                configIds.size(), generated);
    }

    /**
     * Generates all missing months for one configuration atomically.
     *
     * @return the number of transactions created
     */
    @Transactional
    public int generateForConfig(UUID configId) {
        RecurringFinance config = recurringRepository.findById(configId).orElse(null);
        if (config == null || !config.isActive()) {
            return 0;
        }
        LocalDate today = LocalDate.now(clock);
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth startMonth = YearMonth.from(config.getStartDate());
        YearMonth endMonth = config.getEndDate() != null
                ? YearMonth.from(config.getEndDate())
                : currentMonth;

        YearMonth lastGenerated = generationRepository
                .findTopByRecurringFinanceIdOrderByPeriodYearDescPeriodMonthDesc(config.getId())
                .map(r -> YearMonth.of(r.getPeriodYear(), r.getPeriodMonth()))
                .orElse(null);
        YearMonth firstMissing = lastGenerated != null ? lastGenerated.plusMonths(1) : startMonth;
        if (firstMissing.isBefore(startMonth)) {
            firstMissing = startMonth;
        }

        int created = 0;
        for (YearMonth period = firstMissing;
             !period.isAfter(endMonth) && !period.isAfter(currentMonth);
             period = period.plusMonths(1)) {
            // Keep the configured day-of-month, clamped to shorter months (e.g. 31 -> 30/28).
            LocalDate txDate = period.atDay(Math.min(config.getStartDate().getDayOfMonth(), period.lengthOfMonth()));
            if (txDate.isAfter(today)) {
                continue; // This month's day has not arrived yet.
            }
            if (generationRepository.existsByRecurringFinanceIdAndPeriodYearAndPeriodMonth(
                    config.getId(), period.getYear(), period.getMonthValue())) {
                continue; // Already generated (idempotency fast path).
            }
            createGeneratedTransaction(config, period, txDate);
            created++;
        }
        return created;
    }

    private void createGeneratedTransaction(RecurringFinance config, YearMonth period, LocalDate txDate) {
        Transaction transaction = new Transaction();
        transaction.setUser(config.getUser());
        transaction.setType(config.getType());
        transaction.setAmount(config.getAmount());
        transaction.setTransactionDate(txDate);
        transaction.setCategory(config.getCategory());
        transaction.setRemarks(config.getRemarks());
        transaction.setSourceType(TransactionSource.RECURRING);
        transaction.setRecurringFinance(config);
        transaction = transactionRepository.save(transaction);

        RecurringGenerationRecord record = new RecurringGenerationRecord();
        record.setRecurringFinance(config);
        record.setPeriodYear(period.getYear());
        record.setPeriodMonth(period.getMonthValue());
        record.setGeneratedTransaction(transaction);
        generationRepository.save(record);
    }
}
