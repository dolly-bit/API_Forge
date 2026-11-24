# APIForge

APIForge is a developer-facing API access platform. It is being built in milestones, evolving from a reliable Spring Boot foundation into API-key management, upstream routing, distributed rate limiting, usage metering, and analytics.

## Current milestone

**Phase 2 — Authentication, authorization, and API keys (implementation branch)**

The platform supports account registration and login, stateless JWT access tokens, role-based authorization, application ownership, and application-scoped API key creation, listing, validation, and revocation.

## Stack

- Java 21 and Spring Boot 3.5
- Maven
- Spring Security and OAuth2 Resource Server JWT support
- PostgreSQL and Flyway
- Redis
- Docker Compose
- JUnit 5 and Testcontainers

## Local development

Prerequisites: JDK 21, Docker Engine/Desktop, and Maven 3.9+.

Copy the example environment file, then start infrastructure. Docker Compose reads the resulting `.env` file automatically:

```powershell
Copy-Item .env.example .env
docker compose up -d
```

Review the port values first if those ports are already used on your machine.

Set the environment variables from `.env.example` in your shell or IDE run configuration, then start the API:

```bash
mvn spring-boot:run
```

Run tests and verification:

```bash
mvn --batch-mode verify
```

Spring Boot does not automatically load a plain `.env` file. Docker Compose reads it automatically; for a locally launched JVM, export the variables in your shell or configure them in the IDE.

## Authentication API

Management endpoints require a JWT access token:

```http
Authorization: Bearer <access-token>
```

### Register

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "developer@example.com",
  "displayName": "API Developer",
  "password": "use-a-long-unique-password"
}
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "developer@example.com",
  "password": "use-a-long-unique-password"
}
```

Registration and login return a short-lived JWT. Passwords are hashed with BCrypt (work factor 12). Access tokens use HS256, an issuer, user UUID subject, role claim, issued-at time, and expiry. The default lifetime is 15 minutes.

### Applications

- `POST /api/v1/applications` — create an application owned by the authenticated user.
- `GET /api/v1/applications` — list only the authenticated user's applications.

### API keys

- `POST /api/v1/applications/{applicationId}/keys` — create an API key.
- `GET /api/v1/applications/{applicationId}/keys` — list key metadata.
- `DELETE /api/v1/applications/{applicationId}/keys/{keyId}` — revoke a key.

An API key is returned in plaintext only when created. APIForge stores only its SHA-256 hash and a short display prefix. The key cannot be retrieved later. Expired and revoked keys fail validation. API keys are scoped to an application owned by the authenticated user.

## Security configuration

Set `JWT_SECRET` to a unique cryptographically random value of at least 32 bytes in every non-local environment. Never reuse the development fallback. Keep health details disabled in externally accessible environments.

New users receive the `USER` role. Administrative role assignment is an operational task and is not exposed as public self-service registration.

## Engineering principles

- Keep configuration externalized; no production secrets in source control.
- Use forward-only Flyway migrations; never edit an applied migration.
- Keep controllers thin and enforce resource ownership in the service/repository layer.
- Keep JWT authentication separate from API-key authentication.
- Never store raw API keys or return them after their initial creation response.
- Test behavior at unit, integration, and concurrency levels as features are added.

## Roadmap

1. Foundation and architecture
2. Authentication, authorization, and API keys
3. Safe upstream routing and gateway behavior
4. Atomic distributed rate limiting with Redis
5. Plans, quotas, and usage events
6. Usage analytics and operational visibility
7. Security, reliability, and performance hardening
8. Deployment, CI/CD, and portfolio documentation

See `docs/` for architecture and milestone notes.