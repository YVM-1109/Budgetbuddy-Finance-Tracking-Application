# BudgetBuddy

A personal finance tracking web application: email/password + Google Sign-In,
income/expense tracking, monthly recurring finances, one monthly budget and
savings target, a 12-calendar-month dashboard, and a general-purpose OpenAI
chatbot (INR only). Canonical specification lives in [docs/](docs/).

## Repository layout

```
budgetbuddy/
├── frontend/   React + Vite + TypeScript SPA (Vercel target)
├── backend/    Spring Boot 4 modular monolith (Render target)
├── docs/       Canonical specification package (PRD, API, data model, ...)
└── README.md
```

## Tech stack

| Layer     | Technology                                                        |
|-----------|-------------------------------------------------------------------|
| Frontend  | React 19, Vite 7, TypeScript, React Router 7, Recharts            |
| Backend   | Spring Boot 4.1 (Java 21), Spring Security 7, Spring Data JPA     |
| Database  | TiDB (MySQL-compatible) in production; H2 (MySQL mode) in dev/test|
| Auth      | JWT (JJWT) + Google Identity Services (server-side verification) |
| AI        | OpenAI Responses API via backend only (key never leaves server)   |

## Local development

Prerequisites: Java 21, Maven 3.9+, Node 20+.

```bash
# Backend (http://localhost:8080)
cd backend
mvn spring-boot:run

# Frontend (http://localhost:5173)
cd frontend
cp .env.example .env.local    # optional: adjust VITE_API_BASE_URL
npm install
npm run dev
```

The backend starts on an in-memory H2 database with Flyway migrations applied
automatically. Copy [backend/.env.example](backend/.env.example) for the
environment variable contract used in real deployments.

## Tests

```bash
cd backend  && mvn test          # integration tests: auth, isolation, recurring idempotency, dashboard
cd frontend && npm run build     # strict typecheck + production build
```

## Deployment (Vercel + Render + TiDB)

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for the full runbook.

- **Frontend → Vercel** (`frontend/`): set `VITE_API_BASE_URL` and
  `VITE_GOOGLE_CLIENT_ID`.
- **Backend → Render** (`backend/`): deploy as a Java web service running
  `mvn spring-boot:run` with profile `render`; set `DATABASE_URL`,
  `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`,
  `OPENAI_API_KEY`, `CORS_ALLOWED_ORIGINS`.
- **Database → TiDB Cloud**: create the database, use the MySQL-compatible
  connection string with TLS; schema is created by Flyway on first boot.

## Specification documents

1. [PRD](docs/BudgetBuddy/PRD.md) — product requirements and acceptance criteria
2. [Technical spec](docs/BudgetBuddy/TECHNICAL_SPEC.md) — architecture and standards
3. [API spec](docs/BudgetBuddy/API_SPEC.md) — endpoint contract
4. [Data model](docs/BudgetBuddy/DATA_MODEL.md) — entities and constraints
5. [UI spec](docs/BudgetBuddy/UI_SPEC.md) — screens and responsive behavior
6. [Security & reliability](docs/BudgetBuddy/SECURITY_AND_RELIABILITY.md)
7. [Implementation plan](docs/BudgetBuddy/IMPLEMENTATION_PLAN.md) — phased gates
8. [Decisions](docs/BudgetBuddy/DECISIONS.md) — approved product decisions
