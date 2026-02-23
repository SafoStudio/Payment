-- from JSONB to TEXT
ALTER TABLE idempotency_keys
ALTER COLUMN response TYPE TEXT;