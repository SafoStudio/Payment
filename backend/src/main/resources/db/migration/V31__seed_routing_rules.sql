-- V31: Seed baseline routing rules for mock providers (V28).
--
-- Goal: Ensure that, following migrations, the `resolve` operation functions end-to-end without the need for manual rule creation.
-- Each (method, currency) pair maps to one mock provider, with a priority of 100 and a weight of 100.
-- Idempotent: Uses `ON CONFLICT (method, currency, provider_id) DO NOTHING`.
--
-- Rules reference providers by their `code` (V28: `mock-card`, `mock-bank`, `mock-wallet`).
-- If a provider has been deleted, the `INSERT ... SELECT` statement will simply not create a rule (as the `JOIN` will return no results).

INSERT INTO routing_rules (method, currency, provider_id, priority, weight, enabled)
SELECT v.method, v.currency, p.id, 100, 100, TRUE
FROM (VALUES
    ('CARD',          'EUR'),
    ('CARD',          'USD'),
    ('BANK_TRANSFER', 'EUR'),
    ('BANK_TRANSFER', 'USD'),
    ('WALLET',        'EUR')
     ) AS v(method, currency)
    JOIN providers p ON p.code = CASE v.method
        WHEN 'CARD'          THEN 'mock-card'
        WHEN 'BANK_TRANSFER' THEN 'mock-bank'
        WHEN 'WALLET'        THEN 'mock-wallet'
    END
    ON CONFLICT (method, currency, provider_id) DO NOTHING;