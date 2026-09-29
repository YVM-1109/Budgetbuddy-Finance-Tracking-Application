# BudgetBuddy --- Implementation Plan

## Phase 0 --- Repository foundation

Create a clean repository structure:

``` text
budgetbuddy/
├── frontend/
├── backend/
├── docs/
├── README.md
└── .gitignore
```

Add: - documentation - environment examples - root README - no committed
secrets

Gate: - both applications can start locally.

## Phase 1 --- Backend foundation

Implement: - Spring Boot project. - configuration profiles. - database
connection. - Flyway migrations. - JPA entities. - repositories. -
exception handling. - validation. - health endpoint.

Gate: - application starts. - migration succeeds. - database health
succeeds.

## Phase 2 --- Authentication

Implement: - User entity. - password hashing. - registration. - local
login. - JWT/session handling. - Google Sign-In credential
verification. - protected routes. - ownership enforcement.

Gate: - local auth works. - Google auth works with configured
credentials. - unauthenticated access is blocked.

## Phase 3 --- Transaction system

Implement: - transaction entity. - CRUD. - filtering. - search. -
sorting. - pagination. - validation. - ownership checks.

Gate: - all CRUD tests pass.

## Phase 4 --- Recurring finances

Implement: - recurring entity. - monthly recurrence. - generation
record/idempotency. - scheduler/reconciliation. - future-only amount
changes. - historical preservation.

Gate: - no duplicate generation. - historical records remain unchanged.

## Phase 5 --- Financial analytics

Implement: - monthly aggregation. - 12-calendar-month series. - budget
calculations. - savings target calculations. - category aggregation. -
recent transactions. - highest spending categories.

Gate: - calculations verified against known test fixtures.

## Phase 6 --- Frontend foundation

Implement: - React/Vite/TypeScript. - routing. - authentication state. -
API client. - shared components. - responsive layout.

Gate: - public and protected routing works.

## Phase 7 --- Core UI

Implement: - login/register. - dashboard. - transactions. - recurring
finances. - profile/budget. - responsive navigation.

Gate: - complete user journey works against local backend.

## Phase 8 --- Chatbot

Implement: - backend OpenAI service. - chat endpoint. - frontend chat
UI. - timeout/error handling. - rate limiting. - disclaimer.

Gate: - API key remains server-side. - successful and failed provider
scenarios are handled.

## Phase 9 --- Security and hardening

Review: - authentication. - authorization. - CORS. - validation. -
secrets. - error handling. - rate limits. - dependency
vulnerabilities. - cross-user isolation.

Gate: - security checklist complete.

## Phase 10 --- Production deployment

Frontend: - Vercel.

Backend: - Render.

Database: - TiDB.

Configure: - production origins. - environment variables. - Google
authorized origins. - OpenAI key. - database TLS/connection. - health
checks.

Gate: - public URL works end-to-end.

## Phase 11 --- Final verification

Test: - fresh registration. - Google login. - transaction lifecycle. -
recurring lifecycle. - dashboard accuracy. - budget warning. - savings
target. - chatbot. - mobile UI. - cold-start behavior.

Only after this gate should the implementation be considered V1
complete.
