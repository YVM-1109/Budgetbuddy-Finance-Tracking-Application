# BudgetBuddy --- Security, Reliability and Testing Specification

## Security

### Authentication

-   Hash passwords using a modern adaptive password hashing algorithm.
-   Never log passwords.
-   Never return password hashes.
-   Use secure token/session handling.
-   Apply rate limiting to authentication and chat endpoints where
    practical.

### Authorization

Every protected request is scoped to authenticated user identity.

Never trust user_id values supplied by the client for authorization.

### Google Sign-In

Verify the Google credential server-side, including issuer,
audience/client ID, signature and expiration.

### Secrets

Never commit: - OpenAI API key. - JWT signing secret/private key. -
Database password. - Google client secrets where applicable.

### Input security

Validate: - amounts - dates - strings - pagination - sorting fields -
chat message length

Use parameterized queries/JPA rather than concatenated SQL.

### Web security

Configure: - CORS explicitly. - HTTPS in production. - secure headers
where appropriate. - CSRF strategy consistent with chosen stateless
authentication design. - safe error responses.

### OpenAI

Keep API key on backend.

Do not send the user's stored transaction data to OpenAI in V1.

### Privacy

Financial data is private to the owning user.

## Reliability

Handle: - database unavailable - OpenAI unavailable - OpenAI timeout -
duplicate recurring generation - concurrent transaction updates - stale
client state - invalid authentication - expired token - Render cold
start - network interruption

## Recurring generation reliability

Generation must be idempotent.

A repeated scheduler invocation must not create duplicate monthly
transactions.

Use a database uniqueness constraint or generation record to enforce
this.

## Testing

### Backend unit tests

-   Authentication validation.
-   Transaction validation.
-   Calculation services.
-   Budget warning logic.
-   Savings target calculation.
-   Recurring generation.
-   Duplicate prevention.
-   Ownership checks.

### Backend integration tests

-   Register/login.
-   Google authentication verification flow with mocked provider.
-   CRUD transaction flow.
-   Recurring generation with database.
-   Dashboard aggregation.
-   Unauthorized access.

### Frontend tests

-   Authentication state.
-   Transaction form validation.
-   Other-category behavior.
-   Dashboard rendering.
-   Budget warning.
-   Savings target progress.
-   API error handling.

### End-to-end tests

At minimum: 1. Register. 2. Login. 3. Create income. 4. Create expense.
5. Verify dashboard. 6. Configure recurring salary/rent. 7. Generate
monthly record. 8. Change recurring amount. 9. Verify historical amount
remains unchanged. 10. Verify future generation uses new amount. 11.
Exceed budget. 12. Verify warning. 13. Use chatbot. 14. Verify API key
is not present in frontend bundle.

## Acceptance gate

Do not declare deployment-ready until: - Build succeeds. - Backend tests
pass. - Frontend build succeeds. - Critical E2E paths pass. - Database
migrations apply cleanly. - Production environment variables are
documented. - CORS works with production frontend origin. -
Authentication works in production. - Cross-user data access tests fail
safely.
