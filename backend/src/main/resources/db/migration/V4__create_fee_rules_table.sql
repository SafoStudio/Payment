-- fee_rules table
CREATE TABLE IF NOT EXISTS fee_rules
(
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name               VARCHAR(100) NOT NULL,
    transaction_type   VARCHAR(50)  NOT NULL,
    fee_type           VARCHAR(20)  NOT NULL,
    fixed_amount       DECIMAL(19, 2),
    percentage         DECIMAL(5, 2),
    min_amount         DECIMAL(19, 2),
    max_amount         DECIMAL(19, 2),
    currency           VARCHAR(3),
    from_wallet_id     UUID REFERENCES wallets (id),
    to_wallet_id       UUID REFERENCES wallets (id),
    is_active          BOOLEAN DEFAULT true,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,

    CONSTRAINT valid_fee_rule CHECK (
(fee_type = 'FIXED' AND fixed_amount IS NOT NULL) OR
(fee_type = 'PERCENTAGE' AND percentage IS NOT NULL) OR
(fee_type = 'MIXED' AND fixed_amount IS NOT NULL AND percentage IS NOT NULL) OR
(fee_type = 'TIERED')
    )
    );

CREATE INDEX idx_fee_rules_transaction_type ON fee_rules(transaction_type, is_active);
CREATE INDEX idx_fee_rules_from_wallet ON fee_rules(from_wallet_id, is_active);
CREATE INDEX idx_fee_rules_to_wallet ON fee_rules(to_wallet_id, is_active);

-- Insert default fee rules
INSERT INTO fee_rules (id, name, transaction_type, fee_type, fixed_amount, percentage, min_amount, max_amount, currency, to_wallet_id, is_active, created_at, updated_at)
VALUES
    -- PERCENTAGE: fixed_amount = NULL, percentage = 1.0
    (uuid_generate_v4(), 'Default Transfer Fee', 'TRANSFER', 'PERCENTAGE', NULL, 1.0, 0.10, 10.00, 'USD', NULL, true, NOW(), NOW()),

    -- FIXED: fixed_amount = 0.50, percentage = NULL
    (uuid_generate_v4(), 'Default Withdrawal Fee', 'WITHDRAWAL', 'FIXED', 0.50, NULL, NULL, NULL, 'USD', NULL, true, NOW(), NOW()),

    -- FIXED: fixed_amount = 0.00 (free), percentage = NULL
    (uuid_generate_v4(), 'Default Top Up Fee', 'TOP_UP', 'FIXED', 0.00, NULL, NULL, NULL, 'USD', NULL, true, NOW(), NOW())
    ON CONFLICT DO NOTHING;