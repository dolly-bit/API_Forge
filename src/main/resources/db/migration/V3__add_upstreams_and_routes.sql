CREATE TABLE upstreams (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES applications(id),
    name VARCHAR(120) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_upstreams_application_name UNIQUE (application_id, name),
    CONSTRAINT uq_upstreams_id_application UNIQUE (id, application_id)
);

CREATE INDEX idx_upstreams_application_created
    ON upstreams(application_id, created_at DESC);

CREATE TABLE api_routes (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES applications(id),
    upstream_id UUID NOT NULL,
    route_key VARCHAR(63) NOT NULL,
    http_method VARCHAR(10) NOT NULL
        CHECK (http_method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'HEAD')),
    upstream_path_prefix VARCHAR(500) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_routes_upstream_application
        FOREIGN KEY (upstream_id, application_id)
        REFERENCES upstreams(id, application_id),
    CONSTRAINT uq_api_routes_application_key_method
        UNIQUE (application_id, route_key, http_method),
    CONSTRAINT chk_api_routes_route_key
        CHECK (route_key ~ '^[a-z][a-z0-9-]{1,62}$')
);

CREATE INDEX idx_api_routes_application_upstream
    ON api_routes(application_id, upstream_id, created_at DESC);

CREATE INDEX idx_api_routes_active_lookup
    ON api_routes(route_key, http_method) WHERE status = 'ACTIVE';
