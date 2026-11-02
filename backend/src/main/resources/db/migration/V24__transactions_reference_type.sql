ALTER TABLE transactions
    ADD COLUMN reference_type VARCHAR(20)
        CHECK (reference_type IN ('PAYMENT', 'REFUND', 'ADJUSTMENT', 'EXTERNAL'));

ALTER TABLE transactions
    ADD CONSTRAINT transactions_reference_type_requires_id
        CHECK (reference_id IS NULL OR reference_type IS NOT NULL);

COMMENT ON COLUMN transactions.reference_type IS
    'Semantic source of reference_id: PAYMENT/REFUND/ADJUSTMENT/EXTERNAL. NULL when no reference.';