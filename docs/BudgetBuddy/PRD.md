# BudgetBuddy --- Product Requirements Document

**Version:** 1.0\
**Status:** Implementation-ready baseline\
**Currency:** INR only\
**Primary deployment target:** Vercel + Render + TiDB

## 1. Product Definition

BudgetBuddy is a personal finance tracking web application. An
authenticated user can record income and expenses, manage recurring
monthly finances, define a monthly spending budget and savings target,
and inspect financial performance through a dashboard.

A separate general-purpose chatbot allows the user to ask financial
questions through the OpenAI API. The chatbot is deliberately not
connected to BudgetBuddy's financial database in V1.

## 2. Goals

-   Provide reliable personal transaction tracking.
-   Make monthly financial position immediately understandable.
-   Provide a 12-calendar-month historical view.
-   Reduce repeated entry for salary, rent, loans, and other monthly
    recurring finances.
-   Provide simple budget and savings-target awareness.
-   Provide a convenient general financial-assistance chatbot.
-   Produce a deployable web application with a clear separation between
    frontend, backend, database, and external AI provider.

## 3. Non-goals

V1 does not include: - Bank account synchronization. - Automatic bank
transaction imports. - Investment portfolio tracking. - Tax filing. -
Category-specific budgets. - Multi-currency accounting. - Database-aware
AI financial analysis. - AI function calling into the user's financial
data. - Forgot-password email recovery. - Complex recurrence frequencies
beyond monthly. - Multi-user/shared household accounts.

## 4. Users and Roles

V1 has one application role: authenticated user.

A user may access only their own profile, transactions, recurring
finance configurations, and dashboard data.

Unauthenticated visitors may access public authentication pages only.

There is no admin dashboard in V1.

## 5. Authentication

### 5.1 Email/password

Users can: - Register with name, email, and password. - Log in using
email and password. - Log out. - Access protected application routes
only after authentication.

Passwords must never be stored in plaintext.

### 5.2 Google Sign-In

Users can register or sign in using Google Identity Services. The
backend must verify the Google credential/token before creating or
authenticating the local BudgetBuddy account.

Google-provided name and email populate the BudgetBuddy account. Google
authentication does not require a separate BudgetBuddy password.

Google Sign-In requires a configured OAuth client ID and authorized
production origins/redirect configuration.

### 5.3 No password recovery

V1 intentionally excludes forgot-password/email-reset functionality.

## 6. User Profile

Each account stores: - Internal user ID - Name - Email - Password hash
when applicable - Authentication/provider metadata as required - Monthly
budget - Monthly savings target - Created timestamp - Updated timestamp

Currency is fixed to INR and is not user-selectable.

## 7. Transactions

A transaction represents one financial event.

Required conceptual fields: - Transaction ID - Owner/user ID -
Transaction type: INCOME or EXPENSE - Amount - Transaction date -
Category - Remarks - Creation timestamp - Update timestamp - Optional
source metadata identifying recurring-generated records

### 7.1 Transaction operations

Authenticated users can: - Create transactions. - View transactions. -
View a transaction's details. - Edit transactions. - Delete
transactions. - Search/filter transactions. - Sort transactions.

### 7.2 Transaction dates

Past, current, and future dates are allowed.

### 7.3 Amount rules

-   Amount must be greater than zero.
-   Amount must be represented with decimal-safe monetary precision.
-   Negative amounts are not entered by users; transaction type
    determines whether the amount is income or expense.
-   Currency is always INR.

### 7.4 Categories

The system provides predefined categories.

Users may select `Other` and provide a custom category description for
that transaction.

V1 does not provide a category-management screen.

## 8. Recurring Finances

Recurring Finances is a separate feature for monthly repeating financial
items.

Examples: - Salary - Rent - Loan - Other recurring income - Other
recurring expense

Each recurring configuration contains: - Recurring finance ID -
Owner/user ID - Income/expense type - Category - Amount - Start date -
Optional end date if supported by the implementation - Remarks -
Active/inactive status - Last-generated period metadata - Created
timestamp - Updated timestamp

### 8.1 Recurrence

V1 supports monthly recurrence only.

### 8.2 Generation behavior

A recurring configuration produces an ordinary transaction for each
applicable monthly period.

Generated transactions become historical records and must remain
independently editable.

Changing a recurring amount affects future generated transactions only.
It must not rewrite historical transactions.

### 8.3 Idempotency

The generation process must prevent duplicate transactions for the same
recurring configuration and calendar period.

The database should enforce uniqueness at the appropriate level in
addition to application-level checks.

### 8.4 Missed periods

If the system was unavailable when a recurring transaction should have
been generated, the implementation must reconcile missed applicable
periods deterministically without creating duplicates. The exact
catch-up policy should be implemented consistently with the chosen
scheduled-job mechanism.

## 9. Budget

The user can define one monthly budget.

Example: - Monthly budget: ₹30,000.

There are no category-specific budgets.

If current-month expenses exceed the budget: - The system displays a
dashboard warning. - Transactions are not blocked. - The user can
continue recording expenses.

The warning should clearly show the amount by which the budget has been
exceeded.

## 10. Savings Target

The user can define one monthly savings target.

Dashboard behavior should show actual monthly savings against the
target.

Example: - Target: ₹10,000 - Actual: ₹7,500 - Progress: 75%

## 11. Financial Calculations

### Monthly net

`Monthly Net = Monthly Income - Monthly Expenses`

Positive value = savings.

Negative value = loss.

### 12-month period

The dashboard uses the current calendar month plus the preceding 11
calendar months.

Example: September 2026 means October 2025 through September 2026.

The period is calendar-month based, not a rolling 365-day window.

### 12-month net

`12-Month Net = Sum of income over period - Sum of expenses over period`

## 12. Dashboard

The authenticated dashboard includes:

### Summary cards

-   Current-month income
-   Current-month expenses
-   Current-month savings/loss
-   Current financial net position/balance
-   Monthly budget
-   Monthly savings target
-   12-month income
-   12-month expenses
-   12-month savings/loss

### Charts

-   Monthly income vs expense for the 12-month period.
-   Monthly savings/loss trend.
-   Expense breakdown by category.

### Supporting information

-   Recent transactions.
-   Highest spending categories.
-   Budget utilization.
-   Savings-target progress.
-   Budget-exceeded warning where applicable.

All dashboard calculations must be derived from the authenticated user's
own data.

## 13. General AI Chatbot

The chatbot is a general-purpose financial conversation interface.

The user may type questions and receive responses from an OpenAI model.

The chatbot does NOT: - Read the user's transactions. - Read the user's
budget. - Read the user's savings target. - Query the database. - Call
BudgetBuddy APIs. - Automatically personalize advice using stored
financial data.

The user may voluntarily type financial details into the conversation.

The OpenAI API key must remain server-side and must never be exposed to
the browser.

The application should provide a concise notice that AI responses are
informational and should not be treated as professional financial
advice.

The chatbot should handle: - Loading state. - Empty state. - API
errors. - Timeouts. - Rate limiting. - User-visible retry behavior.

Conversation persistence is not required for V1 unless implemented as a
low-cost local UI feature. Server-side chat history storage is outside
the core V1 scope.

## 14. UX Direction

The UI should resemble a modern finance SaaS product: - Professional. -
Clean. - Responsive. - Clear financial hierarchy. - Strong dashboard
cards. - Readable charts. - Consistent form controls. - Mobile-friendly
navigation.

Recommended frontend: React + Vite + TypeScript.

## 15. Major Routes

Public: - `/` - `/login` - `/register`

Authenticated: - `/dashboard` - `/transactions` - `/transactions/new` -
`/transactions/:id` - `/recurring` - `/budget` - `/chat` - `/profile`

Exact route names may be adjusted during implementation if behavior
remains equivalent.

## 16. Success Criteria

The V1 product is successful when: 1. A new user can register and
authenticate. 2. Google Sign-In works in production. 3. An authenticated
user can create, edit, delete, filter, sort, and view transactions. 4.
Recurring monthly finances generate transactions without duplication. 5.
Historical generated transactions remain unchanged when future recurring
amounts change. 6. Dashboard calculations are accurate for the user's
data. 7. The 12-month calendar-month view is correct. 8. Budget-overrun
warnings appear correctly. 9. Savings-target progress is accurate. 10.
The chatbot can securely reach OpenAI without exposing the API key. 11.
Users cannot access another user's financial data. 12. Frontend and
backend can be deployed through the intended Vercel/Render/TiDB
topology.

## 17. Acceptance Principles

Every feature must have: - A defined success state. - Validation
behavior. - Failure behavior. - Permission behavior. - Empty/loading
states where relevant. - Testable acceptance criteria.
