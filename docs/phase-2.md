# Phase 2 — Authentication, authorization, and API keys

## Scope

Phase 2 adds a stateless management API security model:

- Platform users authenticate with email and password.
- Passwords are stored as BCrypt hashes with work factor 12.
- Successful registration/login returns a short-lived HS256 JWT.
- JWT validation checks signature, issuer, and standard time claims.
- JWT role claims are converted to Spring Security authorities.
- New accounts always receive USER; public registration cannot choose ADMIN.
- Applications are owned by a user UUID.
- API keys belong to applications and can be created, listed, validated, and revoked.

## Credential separation

JWTs authenticate a human/platform user against management endpoints. API keys identify an application/client and are validated independently. An API key must not authenticate management endpoints, and a JWT is not a substitute for an API key at the future gateway request boundary.

## Database changes

Flyway V2 adds password hash, role, and account status to the existing platform_users table, plus the api_keys table. The existing platform_users and applications tables from V1 remain the source of truth; no parallel user table is introduced.

## API endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | /api/v1/auth/register | Create a USER account and return an access token |
| POST | /api/v1/auth/login | Authenticate and return an access token |
| GET | /api/v1/users/me | Return the authenticated account |
| POST | /api/v1/applications | Create an owned application |
| GET | /api/v1/applications | List owned applications |
| POST | /api/v1/applications/{applicationId}/keys | Create an application API key |
| GET | /api/v1/applications/{applicationId}/keys | List key metadata |
| DELETE | /api/v1/applications/{applicationId}/keys/{keyId} | Revoke a key |

## API key storage

A key is generated from 32 cryptographically random bytes and encoded with URL-safe Base64. The plaintext key is returned only in the create response. PostgreSQL stores its SHA-256 digest and a short prefix. Validation hashes the presented key before lookup. Revoked and expired keys are rejected. The create/list/revoke operations verify that the authenticated user owns the parent application.

## Configuration and operational notes

- JWT secret is supplied through JWT_SECRET and must be at least 32 UTF-8 bytes.
- The checked-in fallback is for local development only; production must override it with a unique random secret.
- Access-token lifetime defaults to 15 minutes and is configurable between 1 and 60 minutes.
- Health component details remain hidden.
- Existing platform user rows from V1 have a null password hash and cannot log in until credentials are provisioned through an explicit migration/administrative workflow.

## Deferred work

- Refresh-token rotation and server-side session revocation.
- Login throttling and account recovery.
- Admin bootstrap/provisioning workflow.
- API key authentication filter on the gateway request path.
- API key scopes/permissions and audit events.
- End-to-end integration tests against PostgreSQL and Redis.
