-- Migrate existing users.role enum values into user_roles, then drop the column.
-- users.role is no longer source of truth; user_roles + roles are.

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.code = COALESCE(u.role::text, 'MERCHANT')
    ON CONFLICT (user_id, role_id) DO NOTHING;

ALTER TABLE users DROP COLUMN role;

DROP INDEX IF EXISTS idx_users_role;