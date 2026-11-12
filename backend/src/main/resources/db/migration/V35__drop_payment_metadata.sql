-- V35: drop payments.metadata.
--
-- The column was added in V32 as a forward-looking slot for adapter request
-- metadata, but it is not populated anywhere and is currently written as a
-- String (VARCHAR) into a jsonb column, which Postgres rejects
-- ("column metadata is of type jsonb but expression is of type character varying").
--
-- Reintroduce it when there is an actual consumer, as Map/JsonNode with a
-- proper jsonb converter, not as a raw String.

ALTER TABLE payments DROP COLUMN metadata;