# BudgetBuddy — Deployment Runbook (Vercel + Render + TiDB)

## 1. Database — TiDB Cloud

1. Create a TiDB Cloud Serverless cluster and a database named `budgetbuddy`.
2. Note the connection endpoint (gateway host, port 4000) and credentials.
3. TiDB requires TLS: append `sslMode=VERIFY_IDENTITY` (serverless) to the JDBC URL.
4. No manual schema work: Flyway applies `V1__init.sql` on first backend boot.
   The migration is MySQL-compatible and idempotent from a clean database.

Backend environment variables (Render):

```
DATABASE_URL=jdbc:mysql://gateway01.<region>.prod.aws.tidbcloud.com:4000/budgetbuddy?sslMode=VERIFY_IDENTITY
DATABASE_USERNAME=<tidb user>
DATABASE_PASSWORD=<tidb password>
JWT_SECRET=<openssl rand -base64 48>
GOOGLE_CLIENT_ID=<oauth client id>
OPENAI_API_KEY=<openai key>
OPENAI_MODEL=gpt-5.5
CORS_ALLOWED_ORIGINS=https://<your-app>.vercel.app
APP_TIMEZONE=Asia/Kolkata
SPRING_PROFILES_ACTIVE=render
```

## 2. Backend — Render

1. Create a Render **Web Service** from the repository, root directory `backend/`.
2. Build command: `mvn -DskipTests package`
   Start command: `mvn spring-boot:run -Dspring-boot.run.profiles=render`
   (or run the packaged jar: `java -jar target/budgetbuddy-backend-0.1.0.jar --spring.profiles.active=render`)
3. Health check path: `/actuator/health`.
4. Add all environment variables from step 1. Render free instances spin down
   after inactivity; the first request after idling will be slow (cold start)
   and startup automatically runs recurring-generation catch-up.

## 3. Frontend — Vercel

1. Import the repository into Vercel with root directory `frontend/`
   (Vite is auto-detected; build `npm run build`, output `dist/`).
2. Environment variables:

```
VITE_API_BASE_URL=https://<your-backend>.onrender.com
VITE_GOOGLE_CLIENT_ID=<oauth client id>
```

3. Add the Vercel domain to `CORS_ALLOWED_ORIGINS` on Render (comma-separated
   list is supported). Never use wildcard origins with credentials.

## 4. Google Sign-In

1. In Google Cloud Console create an OAuth client (type: Web application).
2. Authorized JavaScript origins: `http://localhost:5173` and the Vercel domain.
3. The same client ID goes to `VITE_GOOGLE_CLIENT_ID` (frontend) and
   `GOOGLE_CLIENT_ID` (backend). The backend rejects credentials issued for a
   different audience and verifies signature, issuer and expiry server-side.

## 5. Verification checklist (Phase 10/11 gate)

- [ ] `GET /actuator/health` returns `UP` on Render.
- [ ] Fresh registration and login work against production origins.
- [ ] Google Sign-In works from the Vercel domain.
- [ ] Transaction CRUD works end-to-end.
- [ ] A recurring finance created with a past start date back-fills months
      without duplicates; changing its amount only affects future months.
- [ ] Dashboard totals match recorded data; budget warning appears when
      expenses exceed the budget; savings progress is correct.
- [ ] Chat works with a configured `OPENAI_API_KEY`; the key never appears in
      the frontend bundle or network responses.
- [ ] Cross-user access (transaction ids of another user) returns 404.
- [ ] Mobile layout: bottom navigation, stacked cards, readable charts.
