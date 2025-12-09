-- V1__initial_schema.sql
-- Initial database schema
-- Flyway naming convention: V{version}__{description}.sql

-- Example table (customize for your domain)
CREATE TABLE IF NOT EXISTS example_entity (
    id              BIGSERIAL PRIMARY KEY,
    external_id     UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    tenant_id       VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX idx_example_entity_tenant ON example_entity(tenant_id);
CREATE INDEX idx_example_entity_name ON example_entity(name);
CREATE INDEX idx_example_entity_created_at ON example_entity(created_at);

-- Outbox table for reliable event publishing
CREATE TABLE IF NOT EXISTS outbox_event (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(255) NOT NULL,
    aggregate_id    VARCHAR(255) NOT NULL,
    event_type      VARCHAR(255) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at    TIMESTAMP WITH TIME ZONE,
    retry_count     INT NOT NULL DEFAULT 0,
    last_error      TEXT
);

CREATE INDEX idx_outbox_unprocessed ON outbox_event(created_at) WHERE processed_at IS NULL;
CREATE INDEX idx_outbox_aggregate ON outbox_event(aggregate_type, aggregate_id);

-- Idempotency keys table
CREATE TABLE IF NOT EXISTS idempotency_key (
    key             VARCHAR(255) PRIMARY KEY,
    request_hash    VARCHAR(64) NOT NULL,
    response        JSONB,
    status          VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_idempotency_expires ON idempotency_key(expires_at);

-- Audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL PRIMARY KEY,
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id         VARCHAR(255),
    tenant_id       VARCHAR(100),
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(100) NOT NULL,
    resource_id     VARCHAR(255),
    correlation_id  VARCHAR(100),
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    request_method  VARCHAR(10),
    request_path    TEXT,
    request_body    JSONB,
    response_status INT,
    response_body   JSONB,
    duration_ms     BIGINT,
    metadata        JSONB
);

CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_log_user ON audit_log(user_id, timestamp);
CREATE INDEX idx_audit_log_tenant ON audit_log(tenant_id, timestamp);
CREATE INDEX idx_audit_log_resource ON audit_log(resource_type, resource_id);
CREATE INDEX idx_audit_log_correlation ON audit_log(correlation_id);

-- Distributed locks table (alternative to Redis)
CREATE TABLE IF NOT EXISTS distributed_lock (
    lock_name       VARCHAR(255) PRIMARY KEY,
    lock_holder     VARCHAR(255) NOT NULL,
    acquired_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    metadata        JSONB
);

CREATE INDEX idx_distributed_lock_expires ON distributed_lock(expires_at);
