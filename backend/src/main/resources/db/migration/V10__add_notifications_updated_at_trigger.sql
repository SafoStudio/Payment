-- V10: Add updated_at trigger for notifications.
-- V9 covered wallets, payments, refunds, fee_rules but missed notifications.

CREATE TRIGGER update_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();