-- V32: provider integration — payments and refunds carry method, provider, and outcome.
--
-- payments.method          — required for new rows (routing key); nullable on legacy rows
--                            (pre-V32 payments never had a provider and cannot be re-routed).
-- payments.provider_id     — which provider processed the payment (FK, no cascade: delete provider
--                            is blocked by ProviderService while referenced — we WANT that, an
--                            audit trail must survive a provider removal; use RESTRICT semantics
--                            via default NO ACTION, not CASCADE).
-- payments.provider_reference — opaque external reference returned by the adapter on APPROVED.
-- payments.failure_reason  — decline code / error code from the adapter on DECLINED/ERROR.
-- payments.metadata        — raw request metadata (future-proof; not populated by slice 3).
--
-- refunds.provider_id / provider_reference / failure_reason — same semantics, resolved from the
-- parent payment's provider (refunds do not re-route).
--
-- Indexes on provider_id for lookups ("all payments through provider X").
-- GRANT not needed: V29 ALTER DEFAULT PRIVILEGES covers new tables; columns of existing tables
-- are covered by the original V19 GRANT on ALL TABLES.

ALTER TABLE payments
    ADD COLUMN method VARCHAR(30)
        CHECK (method IS NULL OR method IN ('CARD', 'BANK_TRANSFER', 'WALLET')),
    ADD COLUMN provider_id UUID REFERENCES providers(id),
    ADD COLUMN provider_reference VARCHAR(100),
    ADD COLUMN failure_reason VARCHAR(100),
    ADD COLUMN metadata JSONB;

CREATE INDEX idx_payments_provider_id ON payments(provider_id) WHERE provider_id IS NOT NULL;
CREATE INDEX idx_payments_method ON payments(method) WHERE method IS NOT NULL;

COMMENT ON COLUMN payments.method IS
    'Routing key (CARD/BANK_TRANSFER/WALLET). NULL on pre-V32 rows; new payments must set it.';
COMMENT ON COLUMN payments.provider_id IS
    'Provider that processed the payment. FK NO ACTION: cannot delete a provider referenced by a payment.';
COMMENT ON COLUMN payments.provider_reference IS
    'External reference from adapter on APPROVED. NULL otherwise.';
COMMENT ON COLUMN payments.failure_reason IS
    'Decline/error code from adapter on DECLINED/ERROR. NULL on success.';
COMMENT ON COLUMN payments.metadata IS
    'Reserved for adapter request metadata; not populated by slice 3.';

ALTER TABLE refunds
    ADD COLUMN provider_id UUID REFERENCES providers(id),
    ADD COLUMN provider_reference VARCHAR(100),
    ADD COLUMN failure_reason VARCHAR(100);

CREATE INDEX idx_refunds_provider_id ON refunds(provider_id) WHERE provider_id IS NOT NULL;

COMMENT ON COLUMN refunds.provider_id IS
    'Provider that processed the refund (from the parent payment; refunds do not re-route).';
COMMENT ON COLUMN refunds.provider_reference IS
    'External reference from adapter on APPROVED. NULL otherwise.';
COMMENT ON COLUMN refunds.failure_reason IS
    'Decline/error code from adapter on DECLINED/ERROR. NULL on success.';