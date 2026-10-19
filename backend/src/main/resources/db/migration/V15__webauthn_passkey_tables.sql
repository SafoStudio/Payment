-- WebAuthn/Passkeys storage for Spring Security.
-- DDL adapted from spring-security-web 7.0.5:
--   org/springframework/security/user-entities-schema.sql
--   org/springframework/security/user-credentials-schema-postgres.sql
-- Adjustments: IF NOT EXISTS, timestamptz, FK on user_entity_user_id, index.

create table if not exists user_entities
(
    id           varchar(1000) not null,
    name         varchar(100)  not null,
    display_name varchar(200),
    primary key (id)
    );

create table if not exists user_credentials
(
    credential_id                varchar(1000) not null,
    user_entity_user_id          varchar(1000) not null,
    public_key                   bytea         not null,
    signature_count              bigint,
    uv_initialized               boolean,
    backup_eligible              boolean       not null,
    authenticator_transports     varchar(1000),
    public_key_credential_type   varchar(100),
    backup_state                 boolean       not null,
    attestation_object           bytea,
    attestation_client_data_json bytea,
    created                      timestamptz   default now(),
    last_used                    timestamptz,
    label                        varchar(1000) not null,
    primary key (credential_id),
    constraint fk_user_credentials_user_entity
    foreign key (user_entity_user_id) references user_entities (id) on delete cascade
    );

create index if not exists idx_user_credentials_user_entity
    on user_credentials (user_entity_user_id);