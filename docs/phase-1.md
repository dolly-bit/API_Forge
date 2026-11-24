# Phase 1 — Foundation and architecture

## Scope

Phase 1 creates the Spring Boot runtime and local infrastructure needed for later product capabilities. It deliberately does not implement JWT authentication, API-key issuance, upstream proxying, rate limiting, billing, or analytics.

## Implemented foundation

- Java 21 and Spring Boot 3.5.x Maven application.
- PostgreSQL 16 and Redis 7 local services through Docker Compose.
- Environment-driven database and Redis configuration.
- Flyway-managed initial schema; Hibernate is configured with `ddl-auto=validate`, never schema creation.
- Actuator health, liveness, and readiness probes.
- Versioned application health endpoint at `GET /api/v1/health`.
- Shared API error contract and global handling for validation, malformed request bodies, and unexpected failures.
- Docker image build and GitHub Actions Maven verification workflow.
- A focused MVC test for the health endpoint.

## Start locally

1. Install JDK 21 and Docker Desktop.
2. Copy `.env.example` to `.env` if you want to override defaults.
3. Start dependencies: `docker compose up -d`.
4. Run the service: `mvn spring-boot:run`.
5. Check:
   - `GET http://localhost:8080/api/v1/health`
   - `GET http://localhost:8080/actuator/health`
   - `GET http://localhost:8080/actuator/health/readiness`

Stop dependencies with `docker compose down`. Persistent local data remains in named volumes; use `docker compose down -v` only when you intentionally want to delete it.

## Initial schema decision

Only `platform_users` and `applications` are created in V1. These are the first stable ownership primitives. API keys, upstreams, routes, plans, subscriptions, usage events, and outbox records will be introduced with the feature milestones that define their invariants. This avoids creating speculative tables before the domain behavior is implemented.

## Acceptance checks

- [ ] `mvn verify` succeeds on JDK 21.
- [ ] `docker compose config` succeeds.
- [ ] PostgreSQL and Redis report healthy.
- [ ] Application starts after dependencies are healthy.
- [ ] Flyway applies V1 exactly once.
- [ ] `GET /api/v1/health` returns HTTP 200 and a timestamp.
- [ ] Actuator readiness returns UP when required dependencies are reachable.
- [ ] No credentials other than local-only examples are committed.
