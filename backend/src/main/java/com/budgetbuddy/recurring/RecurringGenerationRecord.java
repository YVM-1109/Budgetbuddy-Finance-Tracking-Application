package com.budgetbuddy.recurring;

import com.budgetbuddy.transaction.Transaction;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Database-level idempotency record: unique
 * (recurring_finance_id, period_year, period_month) prevents duplicate
 * monthly generation even under concurrent scheduler runs (PRD 8.3).
 */
@Entity
@Table(name = "recurring_generation_records",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_gen_recurring_period",
               columnNames = {"recurring_finance_id", "period_year", "period_month"}))
public class RecurringGenerationRecord {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurring_finance_id", nullable = false)
    private RecurringFinance recurringFinance;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_transaction_id", nullable = false)
    private Transaction generatedTransaction;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @PrePersist
    void prePersist() {
        if (generatedAt == null) generatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public RecurringFinance getRecurringFinance() { return recurringFinance; }
    public void setRecurringFinance(RecurringFinance recurringFinance) { this.recurringFinance = recurringFinance; }
    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }
    public Transaction getGeneratedTransaction() { return generatedTransaction; }
    public void setGeneratedTransaction(Transaction generatedTransaction) { this.generatedTransaction = generatedTransaction; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
