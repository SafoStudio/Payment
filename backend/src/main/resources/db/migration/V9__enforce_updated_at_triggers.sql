-- V9: Enforce updated_at via triggers.
-- Application code no longer manages updated_at; the DB does it on every UPDATE.
-- users already has a trigger from V5.

CREATE TRIGGER update_wallets_updated_at
    BEFORE UPDATE ON wallets
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_refunds_updated_at
    BEFORE UPDATE ON refunds
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_fee_rules_updated_at
    BEFORE UPDATE ON fee_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();