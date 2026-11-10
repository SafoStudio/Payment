-- V30: routing domain — routing_rules.
--
-- routing_rules — rules for selecting a provider based on (method, currency). ADMIN-scoped:
-- access controlled via @PreAuthorize (routing:read:all / routing:manage:all). RLS does not apply
-- (admin configuration, not user data), consistent with providers (V27).
--
-- New permission: routing:read:all (read-only access to rules).
-- routing:manage:all was already granted to ADMIN in V20.
--
-- No GRANT required for app_user: the ALTER DEFAULT PRIVILEGES IN SCHEMA public statement from V29
-- covers tables created after V29.

CREATE TABLE routing_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    method VARCHAR(30) NOT NULL
    CHECK (method IN ('CARD', 'BANK_TRANSFER', 'WALLET')),
    currency CHAR(3) NOT NULL,
    provider_id UUID NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
    priority INT NOT NULL DEFAULT 100 CHECK (priority >= 0),
    weight INT NOT NULL DEFAULT 100 CHECK (weight > 0),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT routing_rules_unique UNIQUE (method, currency, provider_id),
    CONSTRAINT routing_rules_currency_upper CHECK (currency = upper(currency))
);

CREATE INDEX idx_routing_rules_lookup
    ON routing_rules(method, currency, priority, weight DESC)
    WHERE enabled;

CREATE INDEX idx_routing_rules_provider_id
    ON routing_rules(provider_id);

COMMENT ON TABLE routing_rules IS
    'Provider selection rules by (method, currency). priority ASC = preferred; weight DESC = tie-break.';
COMMENT ON COLUMN routing_rules.priority IS
    'Lower value = higher precedence. Resolve picks min(priority), tie-break by weight DESC, then created_at ASC.';
COMMENT ON COLUMN routing_rules.weight IS
    'Tie-break among equal priority; higher weight wins. Reserved for weighted selection in a later slice.';
COMMENT ON COLUMN routing_rules.enabled IS
    'Disabled rules are skipped by resolve; kept for audit/history.';
COMMENT ON COLUMN routing_rules.provider_id IS
    'ON DELETE CASCADE: deleting a provider removes its routing rules (ProviderService.delete does not delete them manually).';

INSERT INTO permissions (code, description) VALUES
    ('routing:read:all', 'Read routing rules')
    ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code = 'routing:read:all'
    ON CONFLICT (role_id, permission_id) DO NOTHING;