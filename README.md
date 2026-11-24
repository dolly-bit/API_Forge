# APIForge

APIForge is a developer-facing API access platform. It is being built in milestones, beginning with a reliable Spring Boot foundation and evolving toward API-key management, upstream routing, distributed rate limiting, usage metering, and analytics.

## Current milestone

**Phase 1 — Foundation and architecture**

This repository is currently being initialized. Phase 1 establishes the application skeleton, local infrastructure, database migration workflow, health/readiness checks, validation, and consistent API errors. Authentication, API keys, proxy routing, and rate limiting are intentionally deferred to later phases.

## Planned stack

- Java 21
- Spring Boot 3.x
- Maven
- PostgreSQL
- Redis
- Flyway
- Docker Compose
- Spring Boot Actuator
- JUnit 5 and Testcontainers

## Local development

Prerequisites: JDK 21, Docker Engine/Desktop, and Maven (or use the Maven wrapper once committed).

```bash
docker compose up -d
./mvnw spring-boot:run
```

On Windows PowerShell, use `./mvnw.cmd spring-boot:run`.

The application will expose its API on `http://localhost:8080`. Health checks are available at `/actuator/health` and readiness at `/actuator/health/readiness`.

## Configuration

Copy `.env.example` to `.env` and adjust local values if needed. Never commit real credentials. Spring configuration reads database and Redis settings from environment variables.

## Engineering principles

- Keep configuration externalized; no secrets in source control.
- Use forward-only Flyway migrations for schema changes.
- Keep request validation and error responses consistent.
- Test behavior at unit, integration, and concurrency levels as features are added.
- Document trade-offs and verify claims with reproducible tests and load-test results.

## Roadmap

1. Foundation and architecture
2. Authentication, authorization, and API keys
3. Safe upstream routing and gateway behavior
4. Atomic distributed rate limiting with Redis
5. Plans, quotas, and usage events
6. Usage analytics and operational visibility
7. Security, reliability, and performance hardening
8. Deployment, CI/CD, and portfolio documentation

See `docs/` for the detailed architecture and milestone notes as they are added.