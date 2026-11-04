-- audit_log.table_name is legacy from V1; V3 introduced entity_type and backfilled it.
-- The domain no longer writes table_name; entity_type is the single source of truth.
ALTER TABLE audit_log ALTER COLUMN table_name DROP NOT NULL;