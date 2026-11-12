-- V36: allow ADJUSTMENT in idempotency_keys.operation.
--
-- V13 introduced the operation CHECK as ('TRANSFER','TOP_UP','REFUND','PAYMENT')
-- and TransferService.adjustBalance uses OPERATION_ADJUSTMENT='ADJUSTMENT', so
-- every adjustBalance call failed the CHECK. The 4th operation code is added here;
-- the unique (operation, idempotency_key) constraint is unchanged.

ALTER TABLE idempotency_keys
DROP CONSTRAINT IF EXISTS idempotency_keys_operation_check;

ALTER TABLE idempotency_keys
    ADD CONSTRAINT idempotency_keys_operation_check
        CHECK (operation IN ('TRANSFER', 'TOP_UP', 'REFUND', 'PAYMENT', 'ADJUSTMENT'));