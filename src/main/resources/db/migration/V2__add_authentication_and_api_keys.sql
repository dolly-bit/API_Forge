ALTER TABLE platform_users
    ADD COLUMN password_hash VARCHAR(100),
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE platform_users
    ADD CONSTRAINT chk_platform_users_role CHECK (role IN ('USER', 'ADMIN')),
    ADD CONSTRAINT chk_platform_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED'));

CREATE UNIQUE INDEX uq_platform_users_email_lower
    ON platform_users (LOWER(email));

CREATE TABLE api_keys (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES applications(id),
    name VARCHAR(120) NOT NULL,
    key_prefix VARCHAR(24) NOT NULL,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'REVOKED')),
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_application_created
    ON api_keys(application_id, created_at DESC);

CREATE INDEX idx_api_keys_active_hash
    ON api_keys(key_hash) WHERE status = 'ACTIVE';