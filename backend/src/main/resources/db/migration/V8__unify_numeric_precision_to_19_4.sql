-- V8: Align monetary columns to DECIMAL(19,4).
-- wallets.balance and transactions.amount already use (19,4).
-- payments, refunds, fees, balance_history, fee_rules used (19,2) — align them.
-- exchange_rates.rate stays (19,10); percentages stay (5,2).

ALTER TABLE payments
ALTER COLUMN amount TYPE DECIMAL(19,4);

ALTER TABLE refunds
ALTER COLUMN amount TYPE DECIMAL(19,4);

ALTER TABLE fees
ALTER COLUMN amount TYPE DECIMAL(19,4);

ALTER TABLE balance_history
ALTER COLUMN previous_balance TYPE DECIMAL(19,4),
    ALTER COLUMN new_balance      TYPE DECIMAL(19,4),
    ALTER COLUMN delta            TYPE DECIMAL(19,4);

ALTER TABLE fee_rules
ALTER COLUMN fixed_amount TYPE DECIMAL(19,4),
    ALTER COLUMN min_amount   TYPE DECIMAL(19,4),
    ALTER COLUMN max_amount   TYPE DECIMAL(19,4);