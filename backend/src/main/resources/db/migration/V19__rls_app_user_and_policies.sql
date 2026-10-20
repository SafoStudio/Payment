-- V19: Row Level Security with dedicated runtime role app_user.
-- Migrations run as postgres (owner). Runtime connects as app_user.
-- FORCE ROW LEVEL SECURITY is required: without it, table owner (postgres) bypasses RLS.
--
-- NOTE: app_user password is NOT set here. It is provisioned outside Flyway:
--   ALTER ROLE app_user WITH PASSWORD '<POSTGRES_APP_PASSWORD>';
-- run once per environment by the provisioning step (psql / secret manager / init script).
-- Keeping the secret out of migration history; password rotation does not require flyway clean.
--
-- RLS context (set by RlsConnection per transaction via set_config(..., true)):
--   app.current_user_id  uuid
--   app.role             comma-separated role codes, e.g. 'MERCHANT' or 'MERCHANT,SUPPORT'
-- Policies fail closed: if context is missing, current_setting(..., true) is NULL and
-- rows do not match (NULL = ... and NULL && ARRAY[...] never evaluate to TRUE).

-- 1. Runtime role: no superuser, no bypassrls, no inherit.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_user') THEN
CREATE ROLE app_user NOSUPERUSER NOBYPASSRLS NOINHERIT LOGIN;
END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- 2. Enable + FORCE RLS on tables with ownership columns.
ALTER TABLE wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallets FORCE ROW LEVEL SECURITY;

ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions FORCE ROW LEVEL SECURITY;

ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments FORCE ROW LEVEL SECURITY;

ALTER TABLE refunds ENABLE ROW LEVEL SECURITY;
ALTER TABLE refunds FORCE ROW LEVEL SECURITY;

ALTER TABLE fees ENABLE ROW LEVEL SECURITY;
ALTER TABLE fees FORCE ROW LEVEL SECURITY;

ALTER TABLE balance_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE balance_history FORCE ROW LEVEL SECURITY;

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;

-- 3. Policies.
-- App-wide admin/support bypass: role list contains ADMIN or SUPPORT.
-- Ownership checks use app.current_user_id (uuid).

-- wallets: own rows for MERCHANT/USER; all for ADMIN/SUPPORT.
CREATE POLICY wallet_select ON wallets FOR SELECT
     USING (
         string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN', 'SUPPORT']
           OR user_id = current_setting('app.current_user_id', true)::uuid
     );

CREATE POLICY wallet_insert ON wallets FOR INSERT
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
          OR user_id = current_setting('app.current_user_id', true)::uuid
    );

CREATE POLICY wallet_update ON wallets FOR UPDATE
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
          OR user_id = current_setting('app.current_user_id', true)::uuid
    )
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
          OR user_id = current_setting('app.current_user_id', true)::uuid
    );

CREATE POLICY wallet_delete ON wallets FOR DELETE
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
          OR user_id = current_setting('app.current_user_id', true)::uuid
    );

-- transactions: via wallets (debit or credit belongs to current user).
CREATE POLICY transaction_select ON transactions FOR SELECT
    USING (
       string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN', 'SUPPORT']
        OR EXISTS (
            SELECT 1 FROM wallets w
            WHERE w.id IN (transactions.debit_wallet_id, transactions.credit_wallet_id)
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

CREATE POLICY transaction_insert ON transactions FOR INSERT
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
        OR EXISTS (
            SELECT 1 FROM wallets w
            WHERE w.id IN (transactions.debit_wallet_id, transactions.credit_wallet_id)
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

-- payments: via wallet_id.
CREATE POLICY payment_select ON payments FOR SELECT
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN', 'SUPPORT']
        OR EXISTS (
            SELECT 1 FROM wallets w
            WHERE w.id = payments.wallet_id
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

CREATE POLICY payment_insert ON payments FOR INSERT
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
        OR EXISTS (
            SELECT 1 FROM wallets w
            WHERE w.id = payments.wallet_id
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

-- refunds: via payment -> wallet.
CREATE POLICY refund_select ON refunds FOR SELECT
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN', 'SUPPORT']
        OR EXISTS (
            SELECT 1 FROM payments p
            JOIN wallets w ON w.id = p.wallet_id
            WHERE p.id = refunds.payment_id
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

CREATE POLICY refund_insert ON refunds FOR INSERT
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
        OR EXISTS (
            SELECT 1 FROM payments p
            JOIN wallets w ON w.id = p.wallet_id
            WHERE p.id = refunds.payment_id
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

-- fees: via from_wallet_id or to_wallet_id.
CREATE POLICY fee_select ON fees FOR SELECT
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN', 'SUPPORT']
        OR EXISTS (
             SELECT 1 FROM wallets w
             WHERE w.id IN (fees.from_wallet_id, fees.to_wallet_id)
               AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

CREATE POLICY fee_insert ON fees FOR INSERT
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
        OR EXISTS (
            SELECT 1 FROM wallets w
            WHERE w.id IN (fees.from_wallet_id, fees.to_wallet_id)
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

-- balance_history: append-only. SELECT + INSERT only; no UPDATE/DELETE by design.
CREATE POLICY balance_history_select ON balance_history FOR SELECT
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN', 'SUPPORT']
        OR EXISTS (
            SELECT 1 FROM wallets w
            WHERE w.id = balance_history.wallet_id
            AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

CREATE POLICY balance_history_insert ON balance_history FOR INSERT
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
        OR EXISTS (
            SELECT 1 FROM wallets w
            WHERE w.id = balance_history.wallet_id
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

-- notifications: owner can read/insert/update/delete own; ADMIN bypass on write.
-- SUPPORT is read-only (moderation visibility) but cannot mutate notifications.
CREATE POLICY notification_select ON notifications FOR SELECT
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN', 'SUPPORT']
        OR user_id = current_setting('app.current_user_id', true)::uuid
    );

CREATE POLICY notification_insert ON notifications FOR INSERT
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
          OR user_id = current_setting('app.current_user_id', true)::uuid
    );

CREATE POLICY notification_update ON notifications FOR UPDATE
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
          OR user_id = current_setting('app.current_user_id', true)::uuid
        )
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
          OR user_id = current_setting('app.current_user_id', true)::uuid
    );

CREATE POLICY notification_delete ON notifications FOR DELETE
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
          OR user_id = current_setting('app.current_user_id', true)::uuid
    );