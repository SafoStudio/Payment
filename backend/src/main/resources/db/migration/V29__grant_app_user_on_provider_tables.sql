-- V29: grant app_user access to provider tables and fix the default-privileges gap.
--
-- V19 granted on ALL TABLES existing then; later tables (V27 providers) were not covered.
-- ALTER DEFAULT PRIVILEGES makes future tables created by this role automatically
-- accessible to app_user, so new migrations don't need per-table GRANTs.
-- RLS stays off for provider tables (admin-scoped, @PreAuthorize).

GRANT SELECT, INSERT, UPDATE, DELETE ON providers TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON provider_capabilities TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON provider_credentials TO app_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO app_user;