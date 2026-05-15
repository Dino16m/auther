# Auther

**Authentication microservice built with Kotlin & Ktor**

A production-ready authentication server implementing JWT-based auth with refresh token rotation, OTP-based password reset, and BCrypt password hashing.

## Stack

| Layer | Technology |
|-------|-----------|
| Language | **Kotlin 2.0** |
| Framework | **Ktor 2.3** (Netty) |
| Database | **PostgreSQL** via Ktorm ORM (H2 for tests) |
| Auth | JWT (access + refresh tokens), BCrypt |
| Serialization | kotlinx.serialization + Jackson |
| Validation | Konform (schema-based) |
| Docs | OpenAPI 3.0 / Swagger UI |
| Testing | JUnit 5, Mockito |

## Architecture & Highlights

- **Dual JWT token system** — short-lived access tokens (2h) + long-lived refresh tokens (20h) with HMAC-SHA256 signing
- **Refresh token rotation** — each refresh issues new tokens and invalidates the old; detects replay attacks by blacklisting entire token families
- **In-memory token blacklisting** — supports both per-JTI (JWT ID) and per-family revocation, enabling secure logout and breach detection
- **OTP-based password reset** — time-limited, HMAC-signed one-time codes bound to a specific user and purpose, preventing replay and cross-purpose misuse
- **Secure password storage** — BCrypt with cost factor 14
- **Input validation** — declarative schema validation on DTOs via Konform, validated at construction time
- **Structured error handling** — typed exception hierarchy (Unauthenticated, BadRequest, NotFound, InvalidToken) mapped to proper HTTP status codes via Ktor `StatusPages`

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/auth/register` | — | Create account |
| `POST` | `/auth/login` | — | Login, returns access + refresh tokens |
| `POST` | `/auth/refresh` | — | Rotate refresh token |
| `POST` | `/auth/logout` | — | Blacklist token family |
| `GET` | `/auth/me` | JWT | Get current user profile |
| `POST` | `/auth/password/change` | JWT | Change password |
| `POST` | `/auth/password/reset/request` | — | Request password reset (returns OTP signature) |
| `POST` | `/auth/password/reset` | — | Reset password with OTP + signature |
| `GET` | `/openapi` | — | Swagger UI |

## Getting Started

```bash
./gradlew run
```

Requires PostgreSQL at `localhost:5432/auther`. Configure via `src/main/resources/application.yaml`.

## Testing

```bash
./gradlew test
```

Tests cover OTP generation/validation (expiry, purpose mismatch, tampering) and the password reset flow (non-existent user, existing user, successful reset).
