-- V38: add missing UPDATE/DELETE RLS policies for payments, refunds, transactions.
-- V19 created only SELECT/INSERT policies for these tables; with FORCE RLS,
-- UPDATE/DELETE had no policy and any UPDATE was rejected. Needed because
-- PaymentProcessor/RefundProcessor/TransferService update these rows
-- (markAsProcessing, succeed/fail, complete, Transaction.complete).

CREATE POLICY payment_update ON payments FOR UPDATE
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
        OR EXISTS (
            SELECT 1 FROM wallets w
            WHERE w.id IN (payments.wallet_id, payments.target_wallet_id)
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    )
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
        OR EXISTS (
            SELECT 1 FROM wallets w
            WHERE w.id IN (payments.wallet_id, payments.target_wallet_id)
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    );

CREATE POLICY payment_delete ON payments FOR DELETE
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
    );

CREATE POLICY refund_update ON refunds FOR UPDATE
    USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
        OR EXISTS (
            SELECT 1 FROM payments p JOIN wallets w ON w.id IN (p.wallet_id, p.target_wallet_id)
            WHERE p.id = refunds.payment_id
              AND w.user_id = current_setting('app.current_user_id', true)::uuid
        )
    )
    WITH CHECK (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
        OR EXISTS (
            SELECT 1 FROM payments p JOIN wallets w ON w.id IN (p.wallet_id, p.target_wallet_id)
    WHERE p.id = refunds.payment_id
    AND w.user_id = current_setting('app.current_user_id', true)::uuid
    )
    );

CREATE POLICY refund_delete ON refunds FOR DELETE
USING (
        string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
    );

CREATE POLICY transaction_update ON transactions FOR UPDATE
    USING (
    string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
    OR EXISTS (
    SELECT 1 FROM wallets w
    WHERE w.id IN (transactions.debit_wallet_id, transactions.credit_wallet_id)
    AND w.user_id = current_setting('app.current_user_id', true)::uuid
    )
    )
WITH CHECK (
    string_to_array(current_setting('app.role', true), ',') && ARRAY['ADMIN']
    OR EXISTS (
    SELECT 1 FROM wallets w
    WHERE w.id IN (transactions.debit_wallet_id, transactions.credit_wallet_id)
    AND w.user_id = current_setting('app.current_user_id', true)::uuid
    )
    );