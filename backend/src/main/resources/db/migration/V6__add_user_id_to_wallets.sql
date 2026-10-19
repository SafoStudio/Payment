ALTER TABLE wallets ADD COLUMN user_id UUID;

UPDATE wallets SET user_id = owner_id::uuid
WHERE owner_id ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

ALTER TABLE wallets ADD CONSTRAINT fk_wallets_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE wallets DROP COLUMN owner_id;

CREATE INDEX idx_wallets_user_id ON wallets(user_id);