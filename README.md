# BudgetBuddy 💰

A personal finance tracker for people who want to know where their money
actually goes — built for the Indian context, denominated in rupees, and
honest about what it does and doesn't do.

BudgetBuddy lets you record income and expenses, set up monthly recurring
items (salary, rent, that EMI you keep forgetting), define one monthly budget
and one savings target, and then see the whole picture on a dashboard that
spans the last 12 calendar months. There's also a chatbot for general
financial questions — deliberately walled off from your data, for reasons
explained below. 

DISCLAIMER: The server will close on inactivity and will have to be restarted for a demo, also, the AI chatbot is currently not working due to the API Key having exhausted its tokens.

> **Status: V1 is live.** Frontend on Vercel, API on Render, data in TiDB —
> try it at **https://budgetbuddy-finance-tracking-applic.vercel.app**
> See [Current status](#current-status--an-honest-snapshot) for the
> unvarnished version.

---

## What you can do

**Track transactions.** Income and expense entries with amount, date,
category, and remarks. Dates can be past, present, or future (planning a
big purchase next month? record it now and watch the dashboard react).
Search across remarks and categories, filter by type / category / date
range, sort by anything that matters, paginate through history. Nine
predefined categories plus an "Other" that asks you to describe the custom
category — no dangling, uncategorizable transactions.

**Stop re-entering the same things.** Salary, rent, loan payments,
subscriptions — set them up once as a *recurring finance* and BudgetBuddy
generates a normal, fully-editable transaction for each month. Missed a
month because the server was down? The next startup back-fills the gap
automatically, exactly once. Change the amount later and only *future*
months change — your history is never silently rewritten.

**Set one budget and one savings target.** Spend past the budget and the
dashboard shows a clear warning with the exact amount you've exceeded — but
never blocks you from recording a transaction. Real life doesn't stop at
₹30,000; the app shouldn't pretend it does.

**See the 12-month picture.** Current-month income, expenses, net, budget
utilization and savings progress up top; income-vs-expense bars, a
savings/loss trend line, and an expense-by-category breakdown for the last
12 calendar months below. All the math happens in the database and arrives
display-ready — the frontend doesn't recalculate a single rupee.

**Ask a chatbot general money questions.** "How do I build an emergency
fund?" works. "How much did I spend on food?" doesn't — on purpose. The
assistant has no access to your transactions, budget, or anything else
stored here. The API key lives on the server; your browser never sees it.
Responses come with a disclaimer because that's what honest AI advice looks
like right now.

**Sign in your way.** Email + password, or Google Sign-In. Google
credentials are verified server-side (signature, issuer, audience, expiry,
verified-email) before an account is created or linked — a browser-asserted
email is never trusted as proof of identity.

---

## Under the hood

```
React 19 + Vite + TypeScript          Spring Boot 4.1 (Java 21)
┌─────────────────────────┐   HTTPS   ┌──────────────────────────────┐
│  SPA: dashboard, charts,│ ────────► │  Spring Security (JWT)       │
│  transactions, recurring│  JSON     │  Modular services            │
│  budget, chat           │           │  Spring Data JPA             │
└─────────────────────────┘           │  Recurring scheduler         │
        Vercel                        │  OpenAI Responses API client │
                                      └──────────────┬───────────────┘
                                              Render │
                                                     ▼
                                        TiDB (MySQL-compatible)
                                        Flyway-managed schema
```

A **modular monolith** — one deployable backend with clean package
boundaries (`auth`, `transaction`, `recurring`, `dashboard`, `chat`,
`security`, `common`), thin controllers, business logic in services, and
every database query scoped by owner. No microservices, message brokers, or
Kubernetes: a V1 personal finance app doesn't need them, and pretending
otherwise is how side projects die.

### Decisions worth explaining

- **Idempotency is a database constraint, not a promise.** Recurring
  generation is guarded by a unique index on
  `(recurring_finance_id, period_year, period_month)`. Two scheduler runs,
  a race, a crash mid-pass — duplicates are structurally impossible, not
  just unlikely.
- **The frontend computes nothing authoritative.** Every total, percentage,
  and series on the dashboard is aggregated in SQL. One source of truth,
  no drift between client and server.
- **The chatbot is database-blind by design.** It would be trivial to
  inject your transactions into the prompt. It's also a privacy line we
  chose not to cross in V1 — the assistant gets exactly what you type,
  nothing more.
- **INR only, on purpose.** Multi-currency accounting is a different
  product with a different class of bugs. This one knows its currency.
- **Ownership is enforced in the queries.** Asking for another user's
  transaction is indistinguishable from asking for one that doesn't exist
  — `404`, no information leak.

---

## Tech stack

| Layer      | Choice | Why |
|------------|--------|-----|
| Frontend   | React 19, Vite 7, TypeScript (strict), React Router 7, Recharts | Fast, boring-in-a-good-way, great charts |
| Backend    | Spring Boot 4.1, Spring Security 7, Spring Data JPA, Flyway | Current stable line; modular monolith |
| Database   | TiDB in production, H2 (MySQL mode) in dev/test | Same SQL, zero-friction local runs |
| Auth       | JJWT (HS256) + Google Identity Services | Stateless tokens, verified Google flow |
| AI         | OpenAI Responses API, server-side only | Current recommended interface; key never leaves the backend |

---

## Project layout

```
budgetbuddy/
├── frontend/                 React SPA (Vercel target)
│   └── src/pages/            one file per screen, plain and findable
├── backend/                  Spring Boot API (Render target)
│   └── src/main/java/com/budgetbuddy/
│       ├── auth/             registration, login, Google verification
│       ├── transaction/      CRUD + filtering + aggregation queries
│       ├── recurring/        configs + idempotent generation engine
│       ├── dashboard/        read-side aggregation service
│       ├── chat/             OpenAI client, rate limiting, timeout
│       ├── security/         JWT filter, config, current-user resolution
│       └── common/           error contract, config properties
├── docs/
│   ├── BudgetBuddy/          the canonical specification this was built from
│   └── DEPLOYMENT.md         the full Vercel + Render + TiDB runbook
└── README.md                 you are here
```

---

## Running it locally

You need Java 21, Maven 3.9+, and Node 20+.

```bash
# Terminal 1 — backend on :8080
cd backend
mvn spring-boot:run

# Terminal 2 — frontend on :5173
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and register an account. That's it — no database
to install. The backend runs on an in-memory H2 database in MySQL
compatibility mode, applies its Flyway migrations on boot, and works out of
the box including the dashboard and recurring engine.

Two things to know about local mode:

- **Data is ephemeral.** H2 is in-memory; restarting the backend wipes it.
  Production uses TiDB via the `render` profile — see
  [backend/.env.example](backend/.env.example) for the full variable
  contract (`DATABASE_URL`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`,
  `OPENAI_API_KEY`, `CORS_ALLOWED_ORIGINS`, …).
- **The chatbot needs an OpenAI key** to give real answers. Without one it
  fails gracefully with a clear message (which is itself a tested path).
  Set `OPENAI_API_KEY` as an environment variable before starting the
  backend to enable it — never commit a real key anywhere.

Google Sign-In stays inert until `VITE_GOOGLE_CLIENT_ID` (frontend) and
`GOOGLE_CLIENT_ID` (backend) are set; the login screen says so plainly
instead of showing a broken button.

---

## Testing

```bash
cd backend && mvn test        # 11 integration tests, full HTTP + database
cd frontend && npm run build  # strict typecheck + production bundle
```

The backend suite runs against a real (in-memory) database through real
HTTP calls and covers the things that would actually embarrass us in
production: the full auth flow including duplicate-email and wrong-password
rejections, transaction CRUD with **cross-user isolation** (user B gets a
404 for user A's transaction, every time), recurring back-fill that is
provably idempotent across repeated generation passes, exact dashboard
mathematics — income, expense, net, exceeded-by amount, savings percentage,
the 12-month series shape — and the chat endpoint's auth, validation, and
failure mapping.

What the tests deliberately don't cover yet is listed below. We'd rather
admit a gap than paper over it.

---

## Current status — an honest snapshot

**Verified working** (automated tests + live browser walkthrough, local
and now in production):

- Registration, login, JWT protection, ownership isolation
- Transaction CRUD, filtering, search, sorting, pagination, validation
- Recurring creation, monthly back-fill, idempotent re-runs,
  future-only amount changes
- Dashboard aggregates: current month, 12-month series, category
  breakdown, budget warning, savings progress
- Responsive UI on desktop and mobile; CORS; consistent error contract
- Chat: authentication, input validation, graceful handling of every
  provider failure mode we could produce (unconfigured, unavailable,
  quota exhausted)
- **Production deployment:** Flyway migrated `V1__init.sql` onto TiDB on
  boot, `/actuator/health` reports UP, and a full register → login →
  create-transaction loop ran through the live site — browser → Vercel →
  Render → TiDB — with the data persisting and reading back correctly

**Built but not yet verified** — each blocked on a credential or a live
environment, not on code:

| Item | What's missing |
|------|----------------|
| Chat happy path | The OpenAI account used for testing ran out of credits before a real completion came back. Request plumbing and error mapping are verified; the response parser has yet to see a genuine payload. |
| Google Sign-In | Needs a real OAuth client ID (Google Cloud Console) — the verification code is written and unit-shaped, never exercised against Google. |
| The scheduled job in the wild | Generation runs at 00:10 Asia/Kolkata; only the startup catch-up path (same code) has fired. Needs a live day boundary. |
| Recurring edge cases | End dates, day-31 clamping to short months, deactivation/reactivation — implemented, not yet under test. |

---

## Deployment

That intended topology is now the real one:

| Piece | Where | Notes |
|-------|-------|-------|
| Frontend | Vercel | https://budgetbuddy-finance-tracking-applic.vercel.app, root `frontend/`, SPA rewrite via `vercel.json` |
| Backend | Render (Docker) | https://budgetbuddy-api-cfmp.onrender.com, free instance — cold-starts after ~15 min idle, so the first request can take ~50s |
| Database | TiDB Cloud Starter | Tokyo region; Flyway applies migrations on boot |

The full runbook — environment variable contract, Google OAuth origin
configuration, and a post-deployment verification checklist — lives in
[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md). Secrets live exclusively in
platform environment configuration; nothing sensitive is or belongs in
this repository.

One deployment-specific fix worth knowing about: TiDB reports itself to
Flyway as "MySQL 8.0", and Spring Boot's managed Flyway module ships
without a MySQL database plugin. The backend adds
`org.flywaydb:flyway-mysql` (version-matched to `flyway-core`) to make
migrations run — without it, startup fails with `Unsupported Database:
MySQL 8.0`.

---

## Documentation

This project was built against a written specification, and the spec ships
with the code:

1. [PRD](docs/BudgetBuddy/PRD.md) — what the product is, acceptance criteria
2. [Technical spec](docs/BudgetBuddy/TECHNICAL_SPEC.md) — architecture, standards
3. [API spec](docs/BudgetBuddy/API_SPEC.md) — endpoint contract
4. [Data model](docs/BudgetBuddy/DATA_MODEL.md) — entities, constraints, relationships
5. [UI spec](docs/BudgetBuddy/UI_SPEC.md) — screens and responsive behavior
6. [Security & reliability](docs/BudgetBuddy/SECURITY_AND_RELIABILITY.md)
7. [Implementation plan](docs/BudgetBuddy/IMPLEMENTATION_PLAN.md) — phased gates
8. [Decisions](docs/BudgetBuddy/DECISIONS.md) — every approved product decision, numbered

When the code and the spec disagree, the spec wins — and if the spec is
wrong, we change the spec first and the code second.
