# BudgetBuddy --- Decisions and Assumptions

## Approved product decisions

  -----------------------------------------------------------------------
  ID                                  Decision
  ----------------------------------- -----------------------------------
  DEC-001                             Product name is BudgetBuddy.

  DEC-002                             Authentication supports
                                      email/password and Google Sign-In.

  DEC-003                             No forgot-password feature in V1.

  DEC-004                             Currency is INR only.

  DEC-005                             Categories include predefined
                                      options plus Other/custom text.

  DEC-006                             Recurring finances are a separate
                                      feature.

  DEC-007                             Recurrence is monthly only.

  DEC-008                             Recurring configurations generate
                                      ordinary transaction records.

  DEC-009                             Changing a recurring amount affects
                                      future records only.

  DEC-010                             Transactions support past, current,
                                      and future dates.

  DEC-011                             Dashboard period is current month
                                      plus previous 11 calendar months.

  DEC-012                             User has one monthly budget.

  DEC-013                             No category-specific budgets.

  DEC-014                             User has one monthly savings
                                      target.

  DEC-015                             Budget overrun produces a dashboard
                                      warning but does not block
                                      transactions.

  DEC-016                             Chatbot is general-purpose and
                                      database-independent.

  DEC-017                             OpenAI is the chatbot provider.

  DEC-018                             Frontend direction is modern
                                      finance SaaS.

  DEC-019                             Target deployment is Vercel +
                                      Render + TiDB.

  DEC-020                             Architecture is modular monolith
                                      for V1.
  -----------------------------------------------------------------------

## Assumptions to verify during implementation

### ASM-001 --- TiDB compatibility

TiDB will be used through its MySQL-compatible connectivity layer. Exact
supported JDBC/SQL behavior must be verified against current TiDB
documentation before production deployment.

### ASM-002 --- Authentication token strategy

A secure stateless authentication strategy will be selected that works
reliably with a separate Vercel SPA and Render API.

### ASM-003 --- Recurring catch-up

If a monthly recurring transaction is missed while the service is
unavailable, the backend will reconcile missing periods
deterministically and idempotently.

### ASM-004 --- Deployment cold starts

The UI and API will tolerate Render free-tier cold starts with normal
loading and retry behavior.

## Open questions

No product-level open questions block implementation.

Remaining technical questions are implementation choices to be resolved
by the engineering agent using current documentation without changing
product behavior.
