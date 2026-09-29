# BudgetBuddy --- Data Model

## 1. User

Fields: - id: UUID/string identifier - name: string, required - email:
string, required, unique - password_hash: string, nullable for
Google-only accounts - auth_provider: LOCAL / GOOGLE / LINKED -
google_subject: nullable, unique when present - monthly_budget:
DECIMAL(15,2), required, non-negative - monthly_savings_target:
DECIMAL(15,2), required, non-negative - created_at - updated_at

Constraints: - Email must be unique. - Google subject must be unique
when present. - User cannot access another user's records.

## 2. Transaction

Fields: - id - user_id - type: INCOME / EXPENSE - amount:
DECIMAL(15,2) - transaction_date: DATE - category: string - remarks:
nullable string - source_type: MANUAL / RECURRING -
recurring_finance_id: nullable - created_at - updated_at

Indexes: - `(user_id, transaction_date)` -
`(user_id, type, transaction_date)` -
`(user_id, category, transaction_date)` -
`(recurring_finance_id, transaction_date)` where applicable

Validation: - amount \> 0 - category required - transaction_date
required - type required - user_id required

## 3. RecurringFinance

Fields: - id - user_id - type: INCOME / EXPENSE - amount - category -
remarks - start_date - end_date: nullable - active - created_at -
updated_at

Monthly-only recurrence means no recurrence-expression column is
required for V1.

## 4. RecurringGenerationRecord

A dedicated generation record may be used to guarantee idempotency.

Fields: - id - recurring_finance_id - period_year - period_month -
generated_transaction_id - generated_at

Unique constraint: `(recurring_finance_id, period_year, period_month)`

This provides a strong database-level duplicate-prevention mechanism.

## 5. Optional audit considerations

V1 does not require a full audit-event system.

Creation/update timestamps are required.

If security or operational requirements later justify an audit log,
introduce it through a separate approved decision rather than silently
expanding V1.

## 6. Relationships

``` text
User 1 ─────── * Transaction
User 1 ─────── * RecurringFinance
RecurringFinance 1 ─────── * RecurringGenerationRecord
RecurringGenerationRecord 1 ─────── 1 Transaction
```

## 7. Deletion behavior

Deleting a user must not leave accessible orphaned financial data.

Transaction deletion is a hard delete in V1 unless implementation
chooses soft deletion for a documented reason.

Deleting a recurring configuration must not delete historical generated
transactions.

Deleting/deactivating a recurring configuration stops future generation
only.
