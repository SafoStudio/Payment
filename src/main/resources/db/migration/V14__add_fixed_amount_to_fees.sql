-- V14: Add fixed_amount to fees to preserve MIXED fee breakdown.
-- For MIXED fees, amount = fixed_amount + percentage_of_transaction.
-- Without fixed_amount, audit cannot reconstruct how the amount was calculated.

ALTER TABLE fees
    ADD COLUMN fixed_amount DECIMAL(19,4);

CREATE INDEX idx_fees_type ON fees(type);