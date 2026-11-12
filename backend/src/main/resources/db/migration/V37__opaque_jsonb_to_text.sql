-- V37: store opaque JSON as TEXT, not JSONB.
--
-- Spring Data JDBC writes String fields via setString (VARCHAR) and does not
-- route them through the registered String -> PGobject converter for jsonb
-- columns. The DB then rejects the parameter:
--   column "payload" is of type jsonb but expression is of type character varying
--
-- These columns hold opaque blobs (outbox payload, adapter config, audit
-- snapshots); they are never queried by JSON operators or indexed with GIN.
-- TEXT is the correct storage; JSON parsing happens in the application.
--
-- payments.metadata was dropped in V35, nothing to migrate here.

ALTER TABLE outbox_events
ALTER COLUMN payload TYPE TEXT USING payload::text;

ALTER TABLE providers
ALTER COLUMN config TYPE TEXT USING config::text;

ALTER TABLE audit_log
ALTER COLUMN old_data TYPE TEXT USING old_data::text;

ALTER TABLE audit_log
ALTER COLUMN new_data TYPE TEXT USING new_data::text;