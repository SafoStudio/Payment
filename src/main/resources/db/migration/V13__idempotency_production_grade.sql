-- Adds operation scoping, status, and request fingerprint.

ALTER TABLE idempotency_keys
    ADD COLUMN operation VARCHAR(20) NOT NULL DEFAULT 'TRANSFER',
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUCCEEDED',
    ADD COLUMN request_fingerprint VARCHAR(64);

ALTER TABLE idempotency_keys
    ADD CONSTRAINT idempotency_keys_operation_check
        CHECK (operation IN ('TRANSFER', 'TOP_UP', 'REFUND', 'PAYMENT'));

ALTER TABLE idempotency_keys
    ADD CONSTRAINT idempotency_keys_status_check
        CHECK (status IN ('PROCESSING', 'SUCCEEDED'));

ALTER TABLE idempotency_keys
DROP CONSTRAINT IF EXISTS idempotency_keys_key_unique;

ALTER TABLE idempotency_keys
    ADD CONSTRAINT idempotency_keys_operation_key_unique
        UNIQUE (operation, idempotency_key);

CREATE INDEX idx_idempotency_fingerprint
    ON idempotency_keys (request_fingerprint)
    WHERE request_fingerprint IS NOT NULL;