-- V21: drop unused USER role.
-- Registration creates MERCHANT. No flow assigns USER. Role carries no behavior.
-- Role permissions cascade via role_permissions.ON DELETE CASCADE (V17).

-- Safety: fail loudly if any user still holds USER role (would be a data error).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM user_roles ur
        JOIN roles r ON r.id = ur.role_id
        WHERE r.code = 'USER'
    ) THEN
        RAISE EXCEPTION 'V21: found users with USER role; migrate them before dropping the role';
END IF;
END
$$;

DELETE FROM roles WHERE code = 'USER';