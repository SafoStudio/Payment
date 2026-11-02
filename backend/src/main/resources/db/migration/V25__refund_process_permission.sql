INSERT INTO permissions (code, description) VALUES
    ('refund:process', 'Process (execute) refunds');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code = 'refund:process';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'MERCHANT' AND p.code = 'refund:process';