-- V22: payments select policy checks the merchant wallet as well as the payer wallet.
-- Before: SELECT required payments.wallet_id to belong to the current user.
-- After: SELECT allows payments.wallet_id OR payments.target_wallet_id to belong to the current user.
-- Reason: incoming payments use a system wallet as payer (wallet_id), the merchant wallet is target_wallet_id.

DROP POLICY payment_select ON payments;

CREATE POLICY payment_select ON payments FOR SELECT
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN', 'SUPPORT']
        OR EXISTS (
            SELECT 1 FROM wallets w
            WHERE w.id IN (payments.wallet_id, payments.target_wallet_id)
            AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );