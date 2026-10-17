-- V12: Drop uuid-ossp extension.
-- V11 switched all UUID defaults to gen_random_uuid(), so uuid-ossp is no longer used.

DROP EXTENSION IF EXISTS "uuid-ossp";