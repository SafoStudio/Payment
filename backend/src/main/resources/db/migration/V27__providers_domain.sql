-- V27: providers domain — catalog, capabilities, credentials.
--
-- providers            — external/mock PSP catalog (справочник, RLS не применяется;
--                        доступ через @PreAuthorize: provider:read:own / provider:manage:all).
-- provider_capabilities — matrix method × currency (+ limits) per provider; routing/validation read this.
-- provider_credentials  — secrets, separate aggregate; values never exposed via GraphQL.
--
-- No new permissions: provider:read:own, provider:manage:all, routing:manage:all already granted in V20.

CREATE TABLE providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    adapter_key VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL
    CHECK (status IN ('ACTIVE', 'DEGRADED', 'DISABLED')),
    type VARCHAR(20) NOT NULL
    CHECK (type IN ('CARD', 'BANK_TRANSFER', 'WALLET')),
    config JSONB,
    last_checked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_providers_status ON providers(status);
CREATE INDEX idx_providers_type ON providers(type);

COMMENT ON COLUMN providers.adapter_key IS
    'Resolves ProviderAdapter bean via ProviderAdapterRegistry; decouples catalog from implementation.';
COMMENT ON COLUMN providers.config IS
    'Adapter-specific behavior (mock scenarios, timeouts). Not domain data.';
COMMENT ON COLUMN providers.last_checked_at IS
    'Last health observation; NULL until first check.';

CREATE TABLE provider_capabilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
    method VARCHAR(30) NOT NULL
    CHECK (method IN ('CARD', 'BANK_TRANSFER', 'WALLET')),
    currency CHAR(3) NOT NULL,
    min_amount NUMERIC(19, 2) NOT NULL CHECK (min_amount >= 0),
    max_amount NUMERIC(19, 2) NOT NULL CHECK (max_amount > 0),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT provider_capabilities_unique UNIQUE (provider_id, method, currency),
    CONSTRAINT provider_capabilities_amount_range CHECK (min_amount <= max_amount),
    CONSTRAINT provider_capabilities_currency_upper CHECK (currency = upper(currency))
);

CREATE INDEX idx_provider_capabilities_lookup
    ON provider_capabilities(method, currency)
    WHERE enabled;

CREATE INDEX idx_provider_capabilities_provider_id
    ON provider_capabilities(provider_id);

COMMENT ON TABLE provider_capabilities IS
    'What a provider can process (method × currency + limits). Source for catalog, validation, routing.';

CREATE TABLE provider_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
    credential_key VARCHAR(100) NOT NULL,
    encrypted_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    rotated_at TIMESTAMPTZ,
    CONSTRAINT provider_credentials_unique UNIQUE (provider_id, credential_key)
);

CREATE INDEX idx_provider_credentials_provider_id
    ON provider_credentials(provider_id);

COMMENT ON TABLE provider_credentials IS
    'Provider secrets. encrypted_value is AES-GCM ciphertext; plaintext never persisted or exposed.';
COMMENT ON COLUMN provider_credentials.rotated_at IS
    'Set when value replaced; NULL means never rotated since creation.';