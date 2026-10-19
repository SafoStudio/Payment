-- V11: Switch UUID defaults from uuid_generate_v4() to gen_random_uuid().
-- gen_random_uuid() is built into PostgreSQL 13+ and does not require
-- the uuid-ossp extension. idempotency_keys already uses it (V3).

ALTER TABLE wallets ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE transactions ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE audit_log ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE payments ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE refunds ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE fees ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE balance_history ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE exchange_rates ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE notifications ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE users ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE fee_rules ALTER COLUMN id SET DEFAULT gen_random_uuid();