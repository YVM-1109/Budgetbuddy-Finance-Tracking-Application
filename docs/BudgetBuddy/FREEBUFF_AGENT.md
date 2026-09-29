# BudgetBuddy --- Freebuff Agent Adapter

## Purpose

This document is an execution adapter for Freebuff. It does not replace
the canonical BudgetBuddy specification.

Freebuff is a multi-agent coding environment that can inspect
repositories, plan, edit files, run commands/checks, research
documentation, and review results. citeturn0search0turn0search3

## Authority

Before making implementation decisions, read: 1. `docs/PRD.md` 2.
`docs/TECHNICAL_SPEC.md` 3. `docs/API_SPEC.md` 4. `docs/DATA_MODEL.md`
5. `docs/UI_SPEC.md` 6. `docs/SECURITY_AND_RELIABILITY.md` 7.
`docs/IMPLEMENTATION_PLAN.md`

The canonical documents outrank this adapter.

## Operating rules

1.  Do not invent product behavior when the canonical specification
    already defines it.
2.  Do not silently add V1 features.
3.  Do not remove required behavior for convenience.
4.  If a requirement is technically impossible or contradictory, stop
    and report the conflict before changing the requirement.
5.  Use current official documentation when a framework, platform, API,
    authentication provider, or deployment behavior may have changed.
6.  Keep changes incremental and verifiable.
7.  Run the most relevant tests/checks after each meaningful phase.
8.  Inspect existing files before replacing them.
9.  Preserve working behavior unless a requirement explicitly changes
    it.
10. Never commit secrets.

## Execution workflow

For each phase:

``` text
READ SPEC
  ↓
INSPECT REPOSITORY
  ↓
PLAN SMALL CHANGESET
  ↓
IMPLEMENT
  ↓
RUN TESTS/CHECKS
  ↓
INSPECT RESULT
  ↓
FIX
  ↓
DOCUMENT MATERIAL DECISIONS
  ↓
MOVE TO NEXT PHASE
```

Do not attempt the entire application as one uncontrolled edit.

## Repository discipline

Prefer a monorepo:

``` text
/
├── frontend/
├── backend/
├── docs/
└── README.md
```

Keep frontend and backend configuration separate.

## Research behavior

Before selecting versions or integration details for: - Spring Boot -
Spring Security - Google Identity Services - TiDB - OpenAI - Vercel -
Render

consult current authoritative documentation.

Do not rely on stale code examples.

## Frontend implementation

Use React + Vite + TypeScript unless an explicit approved decision
changes this.

Do not introduce a large UI framework unless it materially improves
implementation speed or consistency.

## Backend implementation

Use Spring Boot with a modular monolithic architecture.

Suggested package boundaries:

``` text
config
security
auth
user
transaction
recurring
dashboard
chat
common
```

Keep controllers thin. Put business logic in services. Keep database
access in repositories.

## Database

Use migrations.

Never manually edit production schema without a migration.

Ensure recurring-generation uniqueness is enforced at database level.

## Security

Treat every request as untrusted.

Enforce ownership in backend queries.

Never trust frontend-provided user IDs.

Never expose: - secrets - password hashes - JWT secrets - internal stack
traces

## Chatbot

The chatbot is intentionally database-independent.

Do not add: - database tools to the LLM - transaction retrieval tools -
function calling for BudgetBuddy data - automatic financial context
injection

unless the canonical PRD is explicitly changed.

The OpenAI credential stays on the backend. OpenAI's current API
documentation uses the Responses API for model interactions and expects
API keys to be kept securely in server-side environments.
citeturn1search4

## Google authentication

Use Google Identity Services on the frontend and verify the credential
on the backend. Google documents that a client ID is required for
Sign-In with Google and that the credential returned to the application
must be handled appropriately. citeturn1search6turn1search8

## Deployment

Frontend: - Vercel.

Backend: - Render.

Database: - TiDB.

Render free web services can spin down after inactivity, so the
application should tolerate cold starts and the UI should provide normal
loading states. citeturn0search7

## Completion gate

Before saying "complete":

-   Build succeeds.
-   Tests pass.
-   No critical TODOs remain.
-   No secrets are committed.
-   Database migrations work from a clean database.
-   Cross-user authorization has been tested.
-   Recurring generation is idempotent.
-   Production configuration is documented.
-   Frontend can reach backend.
-   Backend can reach TiDB.
-   Backend can reach OpenAI when configured.
-   Google Sign-In is configured for production.
-   The application behaves correctly on desktop and mobile.

## When uncertain

Do not guess about product behavior.

Use this priority:

1.  Canonical PRD.
2.  Technical specification.
3.  Approved decisions.
4.  Current official documentation.
5.  Minimal implementation default.

If uncertainty changes observable product behavior, stop and request
clarification.
