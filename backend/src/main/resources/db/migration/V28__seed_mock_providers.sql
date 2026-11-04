-- V28: seed mock providers for local/demo use.
--
-- Three mock adapters (mock-card / mock-bank / mock-wallet) registered in code
-- (ProviderAdapterRegistry). Seeded here so a fresh DB has a visible catalog.
-- Idempotent: ON CONFLICT DO NOTHING on providers.code and on capabilities triple.
--
-- Config contract (parsed by MockScenario): {"behavior":"SUCCESS|DECLINE|TIMEOUT|MALFORMED"}.
-- All seeded with SUCCESS; switch behavior manually to demo failure paths.
--
-- No credentials seeded: secrets must be set via setProviderCredential mutation,
-- never committed to a migration.

INSERT INTO providers (code, name, adapter_key, status, type, config)
VALUES
    ('mock-card',   'Mock Card Provider',   'mock-card',   'ACTIVE', 'CARD',          '{"behavior":"SUCCESS"}'),
    ('mock-bank',   'Mock Bank Provider',   'mock-bank',   'ACTIVE', 'BANK_TRANSFER', '{"behavior":"SUCCESS"}'),
    ('mock-wallet', 'Mock Wallet Provider', 'mock-wallet', 'ACTIVE', 'WALLET',        '{"behavior":"SUCCESS"}')
    ON CONFLICT (code) DO NOTHING;

INSERT INTO provider_capabilities (provider_id, method, currency, min_amount, max_amount, enabled)
SELECT p.id, c.method, c.currency, c.min_amount, c.max_amount, TRUE
FROM providers p
         JOIN (VALUES
            ('mock-card',   'CARD',          'EUR', 1.00,    10000.00),
            ('mock-card',   'CARD',          'USD', 1.00,    10000.00),
            ('mock-bank',   'BANK_TRANSFER', 'EUR', 10.00,   100000.00),
            ('mock-bank',   'BANK_TRANSFER', 'USD', 10.00,   100000.00),
            ('mock-wallet', 'WALLET',        'EUR', 1.00,    5000.00)
) AS c(code, method, currency, min_amount, max_amount)
              ON c.code = p.code
    ON CONFLICT (provider_id, method, currency) DO NOTHING;