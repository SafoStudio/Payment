-- V20: add section-access permissions for navigation and route guards.
--
-- Adds 17 permission codes and grants them to roles:
--   ADMIN    - all 17
--   MERCHANT - own surface (profile, wallet mgmt, providers, developer, payments, notifications, dashboard)
--   SUPPORT  - read-all + dashboard
--   USER     - profile and notifications only
--
-- Scope: INSERT into permissions and role_permissions only.
-- Does not modify existing permissions, existing role_permissions, or RLS policies.

INSERT INTO permissions (code, description) VALUES
    ('user:read:own',         'Read own profile'),
    ('user:manage:all',       'Access users administration section'),
    ('role:manage:all',       'Access roles and permissions section'),
    ('wallet:manage:own',     'Access own wallet management (payouts, top up)'),
    ('provider:read:own',     'Access connected providers'),
    ('provider:manage:all',   'Access provider catalog and configuration'),
    ('routing:manage:all',    'Access routing rules'),
    ('apikey:manage:own',     'Access own API keys'),
    ('webhook:manage:own',    'Access own webhooks'),
    ('developer:read:own',    'Access developer section'),
    ('fee:manage:all',        'Access fee tariff grid'),
    ('audit:read:all',        'Access audit log'),
    ('merchant:read:all',     'Access all merchants'),
    ('merchant:manage:all',   'Access merchant onboarding and management'),
    ('payment:manage:own',    'Access own payments management'),
    ('notification:read:own', 'Access own notifications'),
    ('dashboard:read',        'Access dashboard')
    ON CONFLICT (code) DO NOTHING;

-- ADMIN: all new section-access permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
      'user:read:own', 'user:manage:all', 'role:manage:all',
      'wallet:manage:own',
      'provider:read:own', 'provider:manage:all', 'routing:manage:all',
      'apikey:manage:own', 'webhook:manage:own', 'developer:read:own',
      'fee:manage:all', 'audit:read:all',
      'merchant:read:all', 'merchant:manage:all',
      'payment:manage:own',
      'notification:read:own',
      'dashboard:read'
    )
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- MERCHANT: own surface
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'MERCHANT'
  AND p.code IN (
      'user:read:own',
      'wallet:manage:own',
      'provider:read:own',
      'apikey:manage:own',
      'webhook:manage:own',
      'developer:read:own',
      'payment:manage:own',
      'notification:read:own',
      'dashboard:read'
    )
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SUPPORT: read-all + dashboard
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'SUPPORT'
  AND p.code IN (
      'user:read:own',
      'provider:read:own',
      'developer:read:own',
      'merchant:read:all',
      'notification:read:own',
      'dashboard:read'
    )
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- USER: profile and notifications only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'USER'
  AND p.code IN (
      'user:read:own',
      'notification:read:own'
    )
    ON CONFLICT (role_id, permission_id) DO NOTHING;