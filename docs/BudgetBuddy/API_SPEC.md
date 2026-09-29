# BudgetBuddy --- API Specification

All endpoints return JSON unless otherwise stated.

## Authentication

### POST /api/auth/register

Purpose: create local account.

Request:

``` json
{
  "name": "Yash",
  "email": "user@example.com",
  "password": "example-password"
}
```

Validation: - Name required. - Valid email. - Password meets configured
minimum complexity/length. - Email unique.

Success: `201 Created`

Failure: - `400` validation error - `409` email already exists

### POST /api/auth/login

Authenticates local credentials.

Success returns an authenticated session/token representation.

Failure: - `401` invalid credentials - `429` excessive authentication
attempts where rate limiting is enabled

### POST /api/auth/google

Accepts a Google credential produced by Google Identity Services.

Backend verifies credential before authentication.

Never accept unverified email/name as authentication proof.

## User

### GET /api/users/me

Returns current authenticated user's profile.

### PUT /api/users/me

Updates allowed profile fields: - name - monthly budget - monthly
savings target

Email and authentication provider identity are not freely editable
through this endpoint.

## Transactions

### GET /api/transactions

Supports: - page - size - type - category - startDate - endDate -
search - sort - direction

All results are scoped to authenticated user.

### POST /api/transactions

Creates manual transaction.

Request:

``` json
{
  "type": "EXPENSE",
  "amount": 1250.00,
  "transactionDate": "2026-09-29",
  "category": "Food",
  "remarks": "Dinner"
}
```

### GET /api/transactions/{id}

Returns one transaction if owned by current user.

### PUT /api/transactions/{id}

Updates editable transaction fields.

Historical recurring-generated transactions may be edited like normal
transactions; editing one does not modify the recurring configuration.

### DELETE /api/transactions/{id}

Deletes the selected transaction.

## Recurring Finances

### GET /api/recurring-finances

Returns the authenticated user's recurring configurations.

### POST /api/recurring-finances

Creates a monthly recurring finance.

### PUT /api/recurring-finances/{id}

Updates recurring configuration.

Changing amount affects future generation only.

### DELETE /api/recurring-finances/{id}

Deactivates or deletes the recurring configuration without deleting
historical generated transactions.

## Budget and savings target

### PUT /api/budget

Updates the single monthly budget.

### PUT /api/savings-target

Updates the single monthly savings target.

## Dashboard

### GET /api/dashboard

Returns a dashboard DTO containing: - current-month totals - 12-month
monthly series - category expense aggregation - recent transactions -
highest spending categories - budget utilization - savings target
progress - budget warning state

The backend performs calculations and returns display-ready aggregates.
The frontend should not independently recalculate authoritative
financial totals.

## Chat

### POST /api/chat

Request:

``` json
{
  "message": "How can I save more money each month?"
}
```

The endpoint forwards the user's message to the configured OpenAI model
through the backend.

The endpoint must not automatically append private BudgetBuddy financial
data.

Response:

``` json
{
  "message": "..."
}
```

Errors: - `400` invalid/empty message - `401` unauthenticated - `429`
provider or application rate limit - `502/503` upstream AI provider
failure - `504` timeout

## Error behavior

Use consistent error codes.

Never expose: - SQL - stack traces - internal hostnames - secrets -
provider credentials
