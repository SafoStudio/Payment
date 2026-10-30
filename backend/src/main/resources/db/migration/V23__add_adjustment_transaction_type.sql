-- V23: allow ADJUSTMENT as a transaction type for admin balance corrections.

ALTER TABLE transactions DROP CONSTRAINT transactions_type_check;

ALTER TABLE transactions ADD CONSTRAINT transactions_type_check
    CHECK (type IN ('TRANSFER', 'TOP_UP', 'WITHDRAWAL', 'REFUND', 'ADJUSTMENT'));