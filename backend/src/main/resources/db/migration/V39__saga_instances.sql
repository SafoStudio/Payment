-- V39: saga_instances for payment saga (slice: PAYMENT_SYNC).
-- One saga per (aggregate_type, aggregate_id, saga_type). payload is TEXT, not JSONB
-- (V37: Spring Data JDBC writes String to jsonb as varchar and fails).
-- version is optimistic lock (@Version) because both the HTTP request thread
-- and SagaTimeoutPublisher (scheduled) update the same row.
-- No RLS: service table, like outbox_events / providers. Access only via SagaService.

CREATE TABLE saga_instances (
    id uuid PRIMARY KEY,
    aggregate_type  varchar(32)  NOT NULL,
    aggregate_id    uuid         NOT NULL,
    saga_type       varchar(64)  NOT NULL,
    status          varchar(32)  NOT NULL,
    current_step    varchar(64)  NOT NULL,
    attempts        int          NOT NULL DEFAULT 0,
    next_attempt_at timestamptz  NULL,
    timeout_at      timestamptz  NULL,
    last_error      text         NULL,
    payload         text         NOT NULL,
    version         bigint       NOT NULL DEFAULT 0,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,

CONSTRAINT uq_saga_aggregate UNIQUE (aggregate_type, aggregate_id, saga_type)
);

CREATE INDEX ix_saga_status_next_attempt ON saga_instances (status, next_attempt_at);
CREATE INDEX ix_saga_status_timeout      ON saga_instances (status, timeout_at);