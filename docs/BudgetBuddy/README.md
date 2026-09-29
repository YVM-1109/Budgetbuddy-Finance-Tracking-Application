# BudgetBuddy --- Engineering Specification Package

This package is the canonical implementation specification for
BudgetBuddy, a personal finance tracker web application.

## Canonical intent

BudgetBuddy provides: - Email/password registration and login. - Google
Sign-In. - User-specific finance data. - Income and expense transaction
tracking. - Monthly recurring finances for salary, rent, loans, and
similar items. - A dashboard with financial summaries and charts. - One
monthly budget and one monthly savings target. - A general-purpose
OpenAI chatbot that is not connected to the user's stored financial
data. - INR-only financial tracking. - Production deployment using
Vercel (frontend), Render (backend), and TiDB (database).

## Document authority

1.  Explicit user decisions
2.  Approved requirements in PRD.md
3.  Approved architecture and technical decisions
4.  Acceptance criteria
5.  Constraints
6.  Recommendations
7.  Implementation defaults

If a lower-level document conflicts with a higher-level requirement,
stop and resolve the conflict before implementation.

## Implementation target

The implementation should be suitable for a React/Vite frontend, Java
Spring Boot backend, MySQL-compatible TiDB database, and REST APIs. The
frontend and backend are deployed separately.

## Recommended reading order

1.  PRD.md
2.  TECHNICAL_SPEC.md
3.  API_SPEC.md
4.  DATA_MODEL.md
5.  UI_SPEC.md
6.  SECURITY_AND_RELIABILITY.md
7.  IMPLEMENTATION_PLAN.md
8.  FREEBUFF_AGENT.md
