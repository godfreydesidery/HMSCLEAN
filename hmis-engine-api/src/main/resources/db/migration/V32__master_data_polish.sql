-- ============================================================================
-- Phase 28: master-data polish (PROCESS.md §13, §17.13, §17.14 item 14).
--
-- Adds company profile (singleton), consumables (ward supplies), plus
-- three drug-administration lookups (dosage, route, frequency). Each
-- lookup is a tiny code+name+active+description table; their main job
-- is replacing free-text on prescription / nursing UIs with picklists.
-- Wiring those picklists into the Prescription entity is a future
-- follow-up.
-- ============================================================================

-- ----- Company profile (singleton) ----------------------------------------

CREATE TABLE md_company_profile (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,
    name            VARCHAR(160) NOT NULL,
    short_name      VARCHAR(80),
    address         VARCHAR(255),
    city            VARCHAR(80),
    country         VARCHAR(80),
    phone           VARCHAR(32),
    email           VARCHAR(120),
    website         VARCHAR(120),
    tax_id          VARCHAR(64),
    motto           VARCHAR(500),
    logo_url        VARCHAR(500),
    currency        VARCHAR(3)   NOT NULL DEFAULT 'TZS',
    timezone        VARCHAR(64),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_md_company_profile_uid UNIQUE (uid)
);

-- Seed the row so the UI never sees an empty profile.
INSERT INTO md_company_profile (uid, name, currency,
                                created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000CP1', 'HMIS Engine', 'TZS',
        NOW(), NOW(), 'system', 'system', 0);

-- ----- Consumables --------------------------------------------------------

CREATE TABLE md_consumable (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,
    code            VARCHAR(32)  NOT NULL,
    name            VARCHAR(160) NOT NULL,
    unit_of_measure VARCHAR(32),
    description     VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_md_consumable_uid  UNIQUE (uid),
    CONSTRAINT uk_md_consumable_code UNIQUE (code)
);
CREATE INDEX idx_md_consumable_active ON md_consumable(active);

-- ----- Drug administration lookups ----------------------------------------

CREATE TABLE md_dosage (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,
    code            VARCHAR(32)  NOT NULL,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_md_dosage_uid  UNIQUE (uid),
    CONSTRAINT uk_md_dosage_code UNIQUE (code)
);

CREATE TABLE md_administration_route (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,
    code            VARCHAR(32)  NOT NULL,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_md_admin_route_uid  UNIQUE (uid),
    CONSTRAINT uk_md_admin_route_code UNIQUE (code)
);

CREATE TABLE md_dosing_frequency (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,
    code            VARCHAR(32)  NOT NULL,
    name            VARCHAR(120) NOT NULL,
    times_per_day   INTEGER,
    description     VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_md_dosing_freq_uid  UNIQUE (uid),
    CONSTRAINT uk_md_dosing_freq_code UNIQUE (code)
);

-- Seed the common defaults so the picklist is usable immediately.

INSERT INTO md_administration_route (uid, code, name, description, active,
                                     created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000AR1', 'ORAL',    'Oral / by mouth',  NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000AR2', 'IV',      'Intravenous',      NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000AR3', 'IM',      'Intramuscular',    NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000AR4', 'SC',      'Subcutaneous',     NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000AR5', 'TOPICAL', 'Topical',          NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000AR6', 'INHALED', 'Inhaled',          NULL, TRUE, NOW(), NOW(), 'system', 'system', 0);

INSERT INTO md_dosing_frequency (uid, code, name, times_per_day, description, active,
                                 created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000DF1', 'OD',   'Once a day',           1,    NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DF2', 'BD',   'Twice a day',          2,    NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DF3', 'TDS',  'Three times a day',    3,    NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DF4', 'QID',  'Four times a day',     4,    NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DF5', 'STAT', 'Once, immediately',    1,    NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DF6', 'PRN',  'As needed',            NULL, NULL, TRUE, NOW(), NOW(), 'system', 'system', 0);

INSERT INTO md_dosage (uid, code, name, description, active,
                       created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000DS1', '1TAB', '1 tablet',  NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DS2', '2TAB', '2 tablets', NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DS3', '5ML',  '5 ml',      NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DS4', '10ML', '10 ml',     NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DS5', '1CAP', '1 capsule', NULL, TRUE, NOW(), NOW(), 'system', 'system', 0);
