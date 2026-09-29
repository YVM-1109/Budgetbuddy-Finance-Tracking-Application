# BudgetBuddy --- Technical Specification

## 1. Architecture

Use a modular monolithic backend for V1.

``` text
React + Vite + TypeScript
        |
        | HTTPS REST/JSON
        v
Spring Boot API
        |
        +--> Spring Security
        |
        +--> Application services
        |
        +--> JPA/Hibernate
        |
        +--> Recurring-finance scheduler/reconciliation
        |
        +--> OpenAI integration
        |
        v
TiDB (MySQL-compatible)
```

Do not introduce microservices, message brokers, Redis, Kubernetes, or a
separate AI service for V1.

## 2. Backend

Recommended: - Java 17 or a currently supported LTS Java version
compatible with the selected Spring Boot release. - Spring Boot. -
Spring Web. - Spring Validation. - Spring Security. - Spring Data JPA. -
Hibernate. - MySQL-compatible JDBC driver. - Database migration tool
such as Flyway. - Actuator for health/operational endpoints. - JWT
library compatible with Spring Security.

The exact dependency versions must be selected from current stable
compatible releases during implementation.

## 3. Frontend

Recommended: - React. - Vite. - TypeScript. - React Router. - Axios or
fetch-based API layer. - Recharts or equivalent charting library. - A
lightweight component/UI system if useful.

Avoid adding a heavy state-management framework unless application
complexity actually requires it.

## 4. API Design

Use RESTful JSON APIs.

Suggested base path: `/api`

Public authentication endpoints: - `POST /api/auth/register` -
`POST /api/auth/login` - `POST /api/auth/google`

Authenticated endpoints: - `GET /api/users/me` - `PUT /api/users/me` -
`GET /api/dashboard` - `GET /api/transactions` -
`POST /api/transactions` - `GET /api/transactions/{id}` -
`PUT /api/transactions/{id}` - `DELETE /api/transactions/{id}` -
`GET /api/recurring-finances` - `POST /api/recurring-finances` -
`PUT /api/recurring-finances/{id}` -
`DELETE /api/recurring-finances/{id}` - `PUT /api/budget` -
`PUT /api/savings-target` - `POST /api/chat`

Health: - `GET /actuator/health`

## 5. Authentication Architecture

Use: - Short-lived access JWT. - Secure authentication flow. - Password
hashing using a modern adaptive password hash such as BCrypt/Argon2. -
Backend authorization based on authenticated user identity.

For a SPA, prefer an architecture that minimizes token exposure. If JWT
is stored client-side, use the least risky storage approach compatible
with the implementation and explicitly document the trade-off. Do not
put secrets in source control.

Every protected query must be scoped by authenticated user ID.

## 6. Google Authentication

The frontend uses Google Identity Services.

The Google credential is submitted to the backend.

The backend verifies the credential's signature, issuer, audience/client
ID, expiry, and required identity claims before accepting it.

If a matching local account exists, authenticate it. Otherwise create a
local user using the verified Google identity.

Do not trust a browser-submitted email address as proof of Google
identity.

## 7. Data Isolation

Every financial entity must contain or be directly associated with an
owner user.

Repository queries must include ownership constraints.

A request for `/transactions/{id}` must return not-found/unauthorized
behavior if the transaction does not belong to the authenticated user.

Do not rely only on frontend filtering for data isolation.

## 8. Monetary Representation

Use decimal numeric database representation, not floating point.

Recommended conceptual type: `DECIMAL(15,2)`

The application should reject invalid monetary precision beyond two
decimal places unless a deliberate rounding policy is defined.

## 9. Date Handling

Transaction dates are calendar dates and should use a date-only
representation rather than timestamp semantics.

Created/updated metadata should use timestamps.

The application's reporting timezone should be explicitly configured and
consistent. For the initial India-focused application, use Asia/Kolkata
unless deployment requirements dictate another canonical strategy.

## 10. Recurring Generation

Implement recurring generation as a deterministic application service.

For each active recurring configuration: 1. Determine applicable
calendar months. 2. Determine which monthly transaction periods have
already been generated. 3. Generate missing transactions. 4. Persist
generation atomically. 5. Enforce uniqueness so repeated scheduler
execution cannot duplicate records.

The implementation must be safe if the scheduler executes more than
once.

The exact scheduler mechanism may use Spring scheduling or an equivalent
robust mechanism.

## 11. Dashboard Querying

Dashboard calculations should be computed from database queries rather
than loading the entire transaction history into memory.

Required aggregation concepts: - Current-month income. - Current-month
expense. - Current-month net. - 12-month monthly income. - 12-month
monthly expense. - 12-month monthly net. - Expense totals by category. -
Recent transactions. - Highest spending categories. - Budget
utilization. - Savings target progress.

Avoid N+1 query patterns.

## 12. OpenAI Integration

The backend owns the OpenAI API credential.

The backend exposes a controlled chat endpoint.

The frontend never receives the OpenAI secret.

Use the current OpenAI API integration supported by the selected
official SDK or HTTP interface. OpenAI's current documentation
recommends the Responses API for model interactions. citeturn1search4

Do not expose arbitrary model selection to unauthenticated clients.

Add reasonable server-side input length limits.

Add timeout and failure handling.

## 13. Error Contract

Use a consistent JSON error shape conceptually similar to:

``` json
{
  "timestamp": "2026-09-29T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "The request contains invalid fields.",
  "path": "/api/transactions",
  "fieldErrors": {
    "amount": "Amount must be greater than zero."
  }
}
```

Do not expose stack traces or internal database errors.

## 14. Deployment

### Frontend

Deploy React/Vite frontend to Vercel. Vercel provides a
zero-configuration deployment path for Vite/React sites.
citeturn0search1

### Backend

Deploy Spring Boot as a Render web service. Render supports Git-based
web-service deployment and free web services, although free services
spin down after inactivity. citeturn0search7

### Database

Use TiDB Cloud with MySQL-compatible connectivity.

The implementation must verify the exact TiDB connection settings, TLS
requirements, JDBC compatibility, and production credentials before
deployment.

### Environment variables

Frontend: - `VITE_API_BASE_URL` - `VITE_GOOGLE_CLIENT_ID`

Backend: - `DATABASE_URL` or equivalent individual DB properties -
`DATABASE_USERNAME` - `DATABASE_PASSWORD` - `JWT_SECRET` or key
material - `GOOGLE_CLIENT_ID` - `OPENAI_API_KEY` -
`CORS_ALLOWED_ORIGINS`

Secrets must only exist in deployment secret/environment configuration.

## 15. CORS

The backend must explicitly allow the deployed Vercel frontend origin.

Development origins may be separately configured.

Do not use wildcard origins with credentials in production.

## 16. Health and Operations

Provide: - Liveness/health endpoint. - Database connectivity health. -
Structured server logs. - Startup configuration validation for required
secrets. - Useful request correlation information.

Do not log passwords, JWTs, OpenAI keys, database passwords, or full
financial records.
