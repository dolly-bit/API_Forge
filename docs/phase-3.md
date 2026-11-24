# Phase 3 — API gateway and upstream routing

## Goal

Turn APIForge from an API-key management service into a working gateway. A client presents an application API key to a stable APIForge endpoint; APIForge authenticates that key, resolves an active route, forwards the request to its configured upstream, and returns a controlled upstream response.

This phase is deliberately a bounded reverse-proxy implementation, not a general-purpose open proxy.

## Request model

- Gateway entry point: `/gateway/{routeKey}/**`.
- Clients authenticate with `X-API-Key: af_live_...`. JWTs remain reserved for management endpoints.
- Each route belongs to an application and maps a unique route key and HTTP method to an upstream base URL and upstream path prefix.
- Route lookup and API-key validation are separate checks. A valid key may access only routes belonging to the key's application.
- Only active applications, active API keys, and active routes are eligible.
- The gateway forwards the remaining path and query string, plus a deliberately selected set of end-to-end request headers and the request body.
- Hop-by-hop headers, the incoming API key, and platform credentials must never be forwarded upstream.

## Planned implementation slices

### 3A — Upstream and route management
- Flyway V3 tables for upstream definitions and route definitions.
- Owner-scoped management endpoints protected by JWT.
- Route uniqueness and referential integrity enforced by PostgreSQL.
- Validate route keys, HTTP methods, URLs, and upstream configuration.
- Restrict upstream destinations with a configurable host allowlist. Do not create an unrestricted proxy or permit arbitrary internal/private destinations by default.
- Unit and PostgreSQL integration tests for ownership, validation, uniqueness, and lifecycle.

### 3A implementation notes

Management endpoints are JWT-protected:

- `POST/GET /api/v1/applications/{applicationId}/upstreams`
- `PATCH /api/v1/applications/{applicationId}/upstreams/{upstreamId}/status`
- `POST/GET /api/v1/applications/{applicationId}/upstreams/{upstreamId}/routes`
- `PATCH /api/v1/applications/{applicationId}/upstreams/{upstreamId}/routes/{routeId}/status`

Upstream destinations require HTTPS and an exact hostname in `GATEWAY_ALLOWED_HOSTS` (comma-separated). An empty allowlist denies all upstream creation. IP literals, localhost, and common local-only host suffixes are rejected. Route uniqueness is enforced per application, route key, and HTTP method; PostgreSQL also enforces that a route's upstream belongs to the same application.

### 3B — Gateway authentication and proxying
- Stateless API-key authentication at the gateway boundary using `X-API-Key`.
- Resolve key -> application -> active route; reject invalid, expired, revoked, suspended, or cross-application access.
- Forward supported HTTP methods, path suffix, query parameters, headers, and bodies.
- Apply connection/read timeouts and a bounded request-body size.
- Filter hop-by-hop headers and never leak API keys upstream or in logs.
- Map upstream timeout/unavailability and invalid upstream responses to stable API errors.
- Integration tests with a local mock upstream; verify both successful forwarding and failure behavior.

## Security and reliability constraints

- No arbitrary destination URL supplied by the gateway caller.
- No forwarding of `Authorization`, `Cookie`, `X-API-Key`, or hop-by-hop headers by default.
- Do not log raw API keys, request bodies, or upstream secrets.
- Do not expose internal exception messages to clients.
- Enforce application ownership on every management query.
- Use forward-only Flyway migrations; never edit V1 or V2 after application.
- Keep JWT management authentication separate from API-key gateway authentication.

## Acceptance criteria

- A developer can create an upstream and route through authenticated management APIs.
- A request with a valid API key reaches only a route belonging to that key's application.
- Invalid, revoked, expired, or suspended credentials/resources are rejected before upstream forwarding.
- Route matching is deterministic and method-aware.
- Query strings and supported request bodies are forwarded correctly.
- Sensitive and hop-by-hop headers are not forwarded.
- Timeouts and upstream failures produce stable, documented responses.
- `mvn verify` passes locally and in GitHub Actions, including PostgreSQL-backed tests and mock-upstream gateway tests.
