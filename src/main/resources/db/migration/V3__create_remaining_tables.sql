-- Payments table
CREATE TABLE IF NOT EXISTS payments
(
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    wallet_id         UUID           NOT NULL REFERENCES wallets (id),
    target_wallet_id  UUID           NOT NULL REFERENCES wallets (id),
    amount            DECIMAL(19, 2) NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    type              VARCHAR(20)    NOT NULL,
    idempotency_key   VARCHAR(255) UNIQUE,
    description       TEXT,
    transaction_id    UUID REFERENCES transactions (id),
    created_at        TIMESTAMP      NOT NULL,
    updated_at        TIMESTAMP      NOT NULL
    );

CREATE INDEX idx_payments_wallet_id ON payments(wallet_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_idempotency_key ON payments(idempotency_key);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);

-- Refunds table
CREATE TABLE IF NOT EXISTS refunds
(
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id             UUID           NOT NULL REFERENCES payments (id),
    transaction_id         UUID REFERENCES transactions (id),
    amount                 DECIMAL(19, 2) NOT NULL,
    currency               VARCHAR(3)     NOT NULL,
    status                 VARCHAR(20)    NOT NULL,
    reason                 TEXT,
    original_transaction_id UUID           NOT NULL REFERENCES transactions (id),
    refund_transaction_id  UUID REFERENCES transactions (id),
    created_at             TIMESTAMP      NOT NULL,
    updated_at             TIMESTAMP      NOT NULL
    );

CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX idx_refunds_status ON refunds(status);

-- Fees table
CREATE TABLE IF NOT EXISTS fees
(
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id     UUID           NOT NULL REFERENCES transactions (id),
    from_wallet_id     UUID           NOT NULL REFERENCES wallets (id),
    to_wallet_id       UUID           NOT NULL REFERENCES wallets (id),
    amount             DECIMAL(19, 2) NOT NULL,
    currency           VARCHAR(3)     NOT NULL,
    type               VARCHAR(20)    NOT NULL,
    percentage         DECIMAL(5, 2),
    calculation_rule   VARCHAR(100),
    created_at         TIMESTAMP      NOT NULL
    );

CREATE INDEX idx_fees_transaction_id ON fees(transaction_id);

-- Balance history table
CREATE TABLE IF NOT EXISTS balance_history
(
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    wallet_id        UUID           NOT NULL REFERENCES wallets (id),
    previous_balance DECIMAL(19, 2) NOT NULL,
    new_balance      DECIMAL(19, 2) NOT NULL,
    delta            DECIMAL(19, 2) NOT NULL,
    transaction_id   UUID           NOT NULL REFERENCES transactions (id),
    operation_type   VARCHAR(50)    NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    created_at       TIMESTAMP      NOT NULL
    );

CREATE INDEX idx_balance_history_wallet_id ON balance_history(wallet_id);
CREATE INDEX idx_balance_history_transaction_id ON balance_history(transaction_id);
CREATE INDEX idx_balance_history_created_at ON balance_history(created_at);

-- Exchange rates table
CREATE TABLE IF NOT EXISTS exchange_rates
(
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_currency  VARCHAR(3)      NOT NULL,
    to_currency    VARCHAR(3)      NOT NULL,
    rate           DECIMAL(19, 10) NOT NULL,
    inverse_rate   DECIMAL(19, 10) NOT NULL,
    valid_from     TIMESTAMP       NOT NULL,
    valid_to       TIMESTAMP,
    source         VARCHAR(50)     NOT NULL,
    created_at     TIMESTAMP       NOT NULL,
    UNIQUE(from_currency, to_currency, valid_from)
    );

CREATE INDEX idx_exchange_rates_currencies ON exchange_rates(from_currency, to_currency);
CREATE INDEX idx_exchange_rates_valid_from ON exchange_rates(valid_from);

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications
(
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    wallet_id     UUID REFERENCES wallets (id),
    user_id       UUID,
    type          VARCHAR(50)  NOT NULL,
    title         VARCHAR(255) NOT NULL,
    content       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    retry_count   INTEGER      DEFAULT 0,
    scheduled_for TIMESTAMP,
    sent_at       TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
    );

CREATE INDEX idx_notifications_wallet_id ON notifications(wallet_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);

-- Audit log update table
ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS user_id UUID,
    ADD COLUMN IF NOT EXISTS user_role VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45),
    ADD COLUMN IF NOT EXISTS user_agent TEXT;

UPDATE audit_log SET entity_type = table_name WHERE entity_type IS NULL;

ALTER TABLE audit_log ALTER COLUMN entity_type SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON audit_log(entity_type, record_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);

-- Idempotency_keys update table
ALTER TABLE idempotency_keys ADD COLUMN id UUID DEFAULT gen_random_uuid();
ALTER TABLE idempotency_keys ALTER COLUMN id SET NOT NULL;
ALTER TABLE idempotency_keys DROP CONSTRAINT idempotency_keys_pkey;
ALTER TABLE idempotency_keys ADD PRIMARY KEY (id);
ALTER TABLE idempotency_keys ADD CONSTRAINT idempotency_keys_key_unique UNIQUE (idempotency_key);