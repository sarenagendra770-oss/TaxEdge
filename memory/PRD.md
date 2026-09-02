# TaxEdge Backend - PRD

## Problem Statement
Build a Spring Boot + MySQL backend for **TaxEdge** — an India-focused compliance/finance platform — organised into the exact package structure requested (core: config, security, exception, common, audit; modules: auth, user, gst, itr, insurance, loan, tds, document, payment, notification). Mobile-app-first: OTP-based login on a 10-digit mobile number.

## Architecture
- **Runtime**: Java 17 + Spring Boot 3.2, packaged as an executable JAR run by supervisor.
- **DB**: MariaDB 10.11 (MySQL-wire compatible) → schema `taxedge`, user `taxedge/taxedge123`, JPA `ddl-auto=update`.
- **Auth**: JWT (HS384) via jjwt 0.12. Principal = mobile number. Roles: `USER`, `CA`, `ADMIN` (`ROLE_*` authorities).
- **Routing**: All endpoints served under `/api` context path on port `8001`. The frontend consumes the backend via `REACT_APP_BACKEND_URL` (see `frontend/.env`).
- **File storage**: Local filesystem at `/app/backend/storage` (configurable via `STORAGE_LOCATION`).
- **OTP**: STUB provider — every request returns/logs code `123456` (configurable via `OTP_MODE`, `OTP_STUB_CODE`). Persisted in `otp_codes` table with 10-min expiry.

## Modules & endpoints (all prefixed with `/api`)
- **auth**
  `POST /auth/login` `{mobile}` → issues OTP, returns `{success, registered, devOtp, message}`
  `POST /auth/otp/send` — alias of `/auth/login`
  `POST /auth/otp/verify` `{mobile, otp}` → returns `{token, user, registered}`
  `POST /auth/password-login` `{mobile, password}` → returns `{token, user, registered}` (401 if wrong / no password / disabled)
  `POST /auth/register` `{mobile, fullName, email, customerType, dob, pan, aadhaar, address, avatarUrl, password?}` → completes profile
- **user**: `GET/PUT /users/me`, `GET /users`, `GET /users/{id}`, `DELETE /users/{id}` (admin)
- **gst**, **itr**, **insurance**, **loan**, **tds**: full CRUD (`POST/GET/GET-all/GET-id/PUT/DELETE`). Loan also has `PATCH /loans/{id}/status` for CA/ADMIN.
- **document**: `POST /documents` (multipart), `GET /documents`, `GET /documents/{id}`, `GET /documents/{id}/download`, `DELETE`.
- **payment**: `POST /payments/initiate`, `POST /payments/callback` (STUB webhook), `GET /payments`, `GET /payments/{id}`.
- **notification**: `POST /notifications` (CA/ADMIN), `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `DELETE`.
- **health**: `GET /health`.

## What's implemented
- Full folder layout matching problem statement.
- MariaDB installed + auto-started by supervisor; JPA schema auto-created.
- 10 modules with entities, repositories, DTOs, services, controllers (basic CRUD).
- JWT auth + mobile-OTP flow (STUB) + password-login for return users, role-based `@PreAuthorize`, global exception handler, CORS wide-open.
- EMI calculation on loan create/update; unique constraints on user mobile/email, policy number, payment txn id.
- File upload/download for documents.
- Web dashboard SPA with 9 tabs (Overview + 8 CRUD modules) at `/app/frontend/src/App.js`.
- End-to-end tested via testing_agent (iterations 1–4): backend 100%, frontend 100%.

## Backlog (P0/P1/P2)
- **P0**: Real SMS provider (MSG91 / Twilio) — swap `AuthService` STUB branch.
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
- Replace hardcoded backend URLs in the RN screens with the value of `REACT_APP_BACKEND_URL` (see `frontend/.env`) suffixed with `/api`.
- Login flow: `POST /auth/login {mobile}` → OTP screen → `POST /auth/otp/verify {mobile, otp}` → JWT.
- Post-verify: if `registered:false`, route to profile screen and call `POST /auth/register {...}`. Response returns updated JWT.
- Return-user shortcut: `POST /auth/password-login {mobile, password}` (only if the user opted to set a password during register).

## Update — PAN/Aadhaar uniqueness on register
- `POST /auth/register` now rejects a submission with `HTTP 409` when the PAN or Aadhaar is already tied to a **different** mobile number.
- The response `message` reveals only the last 4 digits of the existing mobile, e.g. `"PAN already registered with us. Mobile: ******0004"`.
- Self-update (same mobile) is unaffected — the check excludes the caller's own record.
- Frontend `ProfileForm` maps the message to the corresponding field-level error (pan / aadhaar / email) and also raises a toast.
- Verified via testing_agent iteration 5: backend 5/5, frontend 100%.
