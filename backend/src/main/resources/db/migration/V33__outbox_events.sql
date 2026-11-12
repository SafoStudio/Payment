-- V33: transactional outbox for domain events.
--
-- outbox_events — at-least-once publication of domain events. Written in the same
-- transaction as the aggregate change (Payment/Refund), so "aggregate saved + event
-- recorded" is atomic. A polling publisher (OutboxPublisher) picks PENDING rows with
-- SELECT ... FOR UPDATE SKIP LOCKED, dispatches to handlers by event_type, marks
-- PROCESSED, or reschedules with backoff; after max attempts → DEAD.
--
-- event_id is UNIQUE and is the idempotency key for downstream handlers
-- (AuditOutboxHandler writes audit_log with outbox_event_id; duplicate dispatch is a no-op).
--
-- audit_log.outbox_event_id — links an audit row to the outbox event that produced it.
-- UNIQUE partial index makes handler idempotent under retry.

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL UNIQUE,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED', 'DEAD')),
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events(next_attempt_at)
    WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events(aggregate_type, aggregate_id, created_at DESC);

COMMENT ON TABLE outbox_events IS
    'Transactional outbox: domain events written atomically with aggregate changes; published at-least-once by OutboxPublisher.';
COMMENT ON COLUMN outbox_events.event_id IS
    'Idempotency key for downstream handlers; UNIQUE.';
COMMENT ON COLUMN outbox_events.status IS
    'PENDING/FAILED → picked up by publisher; PROCESSED → done; DEAD → max attempts exhausted, manual retry required.';
COMMENT ON COLUMN outbox_events.next_attempt_at IS
    'Backoff schedule: publisher only picks rows with next_attempt_at <= now().';

ALTER TABLE audit_log
    ADD COLUMN outbox_event_id UUID;

CREATE UNIQUE INDEX uq_audit_log_outbox_event_id
    ON audit_log(outbox_event_id)
    WHERE outbox_event_id IS NOT NULL;