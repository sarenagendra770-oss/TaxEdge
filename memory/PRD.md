# TaxEdge Backend - PRD

## Problem Statement
Build a Spring Boot + MySQL backend for **TaxEdge** — an India-focused compliance/finance platform — organised into the exact package structure requested (core: config, security, exception, common, audit; modules: auth, user, gst, itr, insurance, loan, tds, document, payment, notification). Mobile-app-first: OTP-based login on a 10-digit mobile number.

## Architecture
- **Runtime**: Java 17 + Spring Boot 3.2, packaged as an executable JAR run by supervisor.
- **DB**: MariaDB 10.11 (MySQL-wire compatible) → schema `taxedge`, user `taxedge/taxedge123`, JPA `ddl-auto=update`.
- **Auth**: JWT (HS384) via jjwt 0.12. Principal = mobile number. Roles: `USER`, `CA`, `ADMIN` (`ROLE_*` authorities).
- **Routing**: All endpoints served under `/api` context path on port `8001`. K8s ingress fronts external URL `https://compliance-hub-1046.preview.emergentagent.com`.
- **File storage**: Local filesystem at `/app/backend/storage` (configurable via `STORAGE_LOCATION`).
- **OTP**: STUB provider — every request returns/logs code `123456` (configurable via `OTP_MODE`, `OTP_STUB_CODE`). Persisted in `otp_codes` table with 10-min expiry.

## Modules & endpoints (all prefixed with `/api`)
- **auth**  
  `POST /auth/login` `{mobile}` → issues OTP, returns `{success, registered, devOtp, message}`  
  `POST /auth/otp/send` — alias of `/auth/login`  
  `POST /auth/otp/verify` `{mobile, otp}` → returns `{token, user, registered}`  
  `POST /auth/register` `{mobile, fullName, email, customerType, dob, pan, aadhaar, address, avatarUrl, password?}` → completes profile, returns `{token, user, registered:true}`
- **user**: `GET/PUT /users/me`, `GET /users`, `GET /users/{id}`, `DELETE /users/{id}` (admin)
- **gst**, **itr**, **insurance**, **loan**, **tds**: full CRUD (`POST/GET/GET-all/GET-id/PUT/DELETE`). Loan also has `PATCH /loans/{id}/status` for CA/ADMIN.
- **document**: `POST /documents` (multipart), `GET /documents`, `GET /documents/{id}`, `GET /documents/{id}/download`, `DELETE`.
- **payment**: `POST /payments/initiate`, `POST /payments/callback` (STUB webhook), `GET /payments`, `GET /payments/{id}`.
- **notification**: `POST /notifications` (CA/ADMIN), `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `DELETE`.
- **health**: `GET /health`.

## What's implemented (2026-01)
- Full folder layout matching problem statement.
- MariaDB installed + auto-started by supervisor; JPA schema auto-created.
- 10 modules with entities, repositories, DTOs, services, controllers (basic CRUD).
- JWT auth + mobile-OTP flow (STUB), role-based `@PreAuthorize`, global exception handler, CORS wide-open for preview.
- EMI calculation on loan create/update; unique constraints on user mobile/email, policy number, payment txn id.
- File upload/download for documents.
- End-to-end smoke tested via curl (health, login → OTP → verify → register → users/me → gst/itr/loan CRUD → role denial).

## Backlog (P0/P1/P2)
- **P0**: Real SMS provider (MSG91 / Twilio) — swap `AuthService` STUB branch.
- **P1**: Password-based login endpoint for OTP-registered users who set a password.
- **P1**: Refresh tokens + logout/token revocation list.
- **P1**: Pagination (`Pageable`) on all `list` endpoints.
- **P2**: Real payment provider (Razorpay/Stripe) with signed webhooks.
- **P2**: Email/SMS notification dispatch (SendGrid + MSG91).
- **P2**: Audit log entity (`AuditableEntity` w/ createdBy/updatedBy via `AuditorAware`).
- **P2**: Actuator + Prometheus metrics; structured JSON logging.
- **P2**: Integration tests with Testcontainers-MySQL.

## Env vars (backend/.env style)
```
DB_URL=jdbc:mysql://localhost:3306/taxedge?...
DB_USER=taxedge
DB_PASSWORD=taxedge123
JWT_SECRET=<base64>
OTP_MODE=STUB
OTP_STUB_CODE=123456
```

## Mobile app integration notes
- Base URL: replace hardcoded `http://localhost:8084` with `https://compliance-hub-1046.preview.emergentagent.com/api`.
- Login screen: `POST /auth/login {mobile}` → route to OTP screen.
- OTP screen: `POST /auth/otp/verify {mobile, otp}` → store `token`; if `registered:false` route to profile screen, else home.
- Profile screen: `POST /auth/register {...full profile}` with `Authorization: Bearer <token>` (optional here; endpoint is open for post-OTP completion). Response returns updated JWT.

## Update — 2026-01 · Password Login
- New endpoint `POST /auth/password-login` `{mobile, password}` → same `ApiResponse<AuthResponse>` envelope as `/auth/otp/verify`. 401 on wrong password, 401 on "no password set" (OTP-only account), 403 on disabled account.
- Frontend login card now has an **OTP / Password** pill toggle (`[data-testid=login-mode-toggle]`). Password fields (`login-password-input`) appear when the Password tab is active; CTA text switches from "Send OTP" to "Sign in". OTP path is unchanged.
- Regression + new tests: **6/6 backend, 100% frontend** (see `/app/test_reports/iteration_4.json`).
