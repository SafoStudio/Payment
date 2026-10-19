-- RBAC core: roles, permissions, role_permissions, user_roles
-- Source of truth for authorization. users.role (enum) is deprecated; dropped in V18.

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) UNIQUE NOT NULL,
    description TEXT
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- No UNIQUE on user_id: users can have multiple roles.
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Seed: roles
INSERT INTO roles (code, name, description) VALUES
    ('ADMIN',   'Administrator', 'Full platform access'),
    ('MERCHANT','Merchant',      'Own wallets, payments, refunds'),
    ('SUPPORT', 'Support',       'Read-only access to all data'),
    ('USER',    'User',          'Basic authenticated user');

-- Seed: permissions
INSERT INTO permissions (code, description) VALUES
    ('user:read:all',        'Read all users'),
    ('user:create',          'Create users'),
    ('user:update',          'Update users'),
    ('user:block',           'Block/activate/delete users'),
    ('user:assign_role',     'Assign roles to users'),
    ('wallet:read:all',      'Read all wallets'),
    ('wallet:read:own',      'Read own wallets'),
    ('wallet:create',        'Create wallets'),
    ('wallet:block',         'Block/close wallets'),
    ('payment:read:all',     'Read all payments'),
    ('payment:read:own',     'Read own payments'),
    ('payment:create',       'Create payments'),
    ('payment:cancel',       'Cancel payments'),
    ('refund:read:all',      'Read all refunds'),
    ('refund:read:own',      'Read own refunds'),
    ('refund:create',        'Create refunds'),
    ('fee:read',             'Read fees'),
    ('fee_rule:manage',      'Manage fee rules'),
    ('transaction:read:all', 'Read all transactions'),
    ('transaction:read:own', 'Read own transactions');

-- Seed: role → permissions
-- ADMIN: everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

-- MERCHANT: own data + create
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'MERCHANT' AND p.code IN (
        'wallet:read:own', 'wallet:create',
        'payment:read:own', 'payment:create',
        'refund:read:own', 'refund:create',
        'transaction:read:own', 'fee:read'
    );

-- SUPPORT: read-only all
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPPORT' AND p.code IN (
        'user:read:all', 'wallet:read:all', 'payment:read:all',
        'refund:read:all', 'transaction:read:all', 'fee:read'
    );

-- USER: minimal
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'USER' AND p.code IN ('wallet:read:own', 'transaction:read:own');