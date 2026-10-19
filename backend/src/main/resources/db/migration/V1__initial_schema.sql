CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id VARCHAR(100) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0 CHECK ( balance >= 0 ),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL  DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT wallets_currency_check CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT wallets_status_check CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
);

CREATE INDEX idx_wallets_owner ON wallets(owner_id);
CREATE INDEX idx_wallets_status ON wallets(status);
CREATE INDEX idx_wallets_created_at ON wallets(created_at);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    debit_wallet_id UUID NOT NULL REFERENCES wallets(id),
    credit_wallet_id UUID NOT NULL REFERENCES wallets(id),
    amount DECIMAL(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    description TEXT,
    reference_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_debit_wallet FOREIGN KEY (debit_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_credit_wallet FOREIGN KEY (credit_wallet_id) REFERENCES wallets(id),
    CONSTRAINT different_wallets CHECK (debit_wallet_id != credit_wallet_id),
    CONSTRAINT transactions_type_check CHECK (type IN ('TRANSFER', 'TOP_UP', 'WITHDRAWAL', 'REFUND')),
    CONSTRAINT transactions_status_check CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_transactions_debit ON transactions(debit_wallet_id, created_at DESC);
CREATE INDEX idx_transactions_credit ON transactions(credit_wallet_id, created_at DESC);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX idx_transactions_reference ON transactions(reference_id) WHERE reference_id IS NOT NULL;

CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    response JSONB NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name VARCHAR(50) NOT NULL,
    record_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_data JSONB,
    new_data JSONB,
    created_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_record ON audit_log(table_name, record_id, created_at DESC);

INSERT INTO wallets (id, owner_id, currency, status, balance) VALUES
    ('00000000-0000-0000-0000-000000000001', 'SYSTEM_REVENUE', 'USD', 'ACTIVE', 0),
    ('00000000-0000-0000-0000-000000000002', 'SYSTEM_FEES', 'USD', 'ACTIVE', 0)
ON CONFLICT (id) DO NOTHING;