-- ============================================================================
-- Patient module + InsurancePlan + polymorphic service-pricing matrix.
-- ============================================================================

-- ----- Insurance plans (sister to md_insurance_provider) -------------------

CREATE TABLE md_insurance_plan (
    id                   BIGSERIAL PRIMARY KEY,
    uid                  VARCHAR(26)  NOT NULL,
    code                 VARCHAR(32)  NOT NULL,
    name                 VARCHAR(200) NOT NULL,
    provider_uid         VARCHAR(26)  NOT NULL,
    covers_consultation  BOOLEAN NOT NULL DEFAULT TRUE,
    covers_lab           BOOLEAN NOT NULL DEFAULT TRUE,
    covers_radiology     BOOLEAN NOT NULL DEFAULT TRUE,
    covers_procedure     BOOLEAN NOT NULL DEFAULT TRUE,
    covers_medicine      BOOLEAN NOT NULL DEFAULT TRUE,
    covers_admission     BOOLEAN NOT NULL DEFAULT TRUE,
    description          VARCHAR(500),
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by           VARCHAR(80),
    updated_by           VARCHAR(80),
    version              BIGINT,
    CONSTRAINT uk_md_insurance_plan_code UNIQUE (code),
    CONSTRAINT uk_md_insurance_plan_uid  UNIQUE (uid)
);
CREATE INDEX idx_md_insurance_plan_provider ON md_insurance_plan(provider_uid);
CREATE INDEX idx_md_insurance_plan_active   ON md_insurance_plan(active);

-- Sample plan tied to the seeded NHIF provider so the UI has something to render.
INSERT INTO md_insurance_plan (uid, code, name, provider_uid,
                               covers_consultation, covers_lab, covers_radiology,
                               covers_procedure, covers_medicine, covers_admission,
                               description, active,
                               created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000IP1', 'NHIF-STD',  'NHIF Standard',  '01J5KQRPCD0000000000000IN1',
     TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
     'Default NHIF scheme', TRUE,
     NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000IP2', 'AAR-GOLD', 'AAR Gold',        '01J5KQRPCD0000000000000IN2',
     TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
     'AAR top-tier scheme', TRUE,
     NOW(), NOW(), 'system', 'system', 0);

-- ----- Patients ------------------------------------------------------------

-- Sequence used by PatientNumberGenerator for human-readable PT-YYYY-NNNNNN
CREATE SEQUENCE patient_no_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE patient (
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(26)  NOT NULL,
    patient_no          VARCHAR(32)  NOT NULL,

    first_name          VARCHAR(80)  NOT NULL,
    middle_name         VARCHAR(80),
    last_name           VARCHAR(80)  NOT NULL,

    date_of_birth       DATE         NOT NULL,
    gender              VARCHAR(16)  NOT NULL,
    patient_type        VARCHAR(16)  NOT NULL,
    payment_type        VARCHAR(16)  NOT NULL,

    insurance_plan_uid  VARCHAR(26),
    membership_no       VARCHAR(64),

    phone_no            VARCHAR(40),
    email               VARCHAR(120),
    address             VARCHAR(255),
    nationality         VARCHAR(80),
    national_id         VARCHAR(64),
    passport_no         VARCHAR(64),

    kin_full_name       VARCHAR(160),
    kin_relationship    VARCHAR(80),
    kin_phone_no        VARCHAR(40),

    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(80),
    updated_by          VARCHAR(80),
    version             BIGINT,
    CONSTRAINT uk_patient_no  UNIQUE (patient_no),
    CONSTRAINT uk_patient_uid UNIQUE (uid)
);
CREATE INDEX idx_patient_last_name   ON patient(last_name);
CREATE INDEX idx_patient_phone_no    ON patient(phone_no);
CREATE INDEX idx_patient_national_id ON patient(national_id);

-- ----- Service pricing matrix ----------------------------------------------

CREATE TABLE md_service_price (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)   NOT NULL,
    plan_uid    VARCHAR(26),
    kind        VARCHAR(32)   NOT NULL,
    service_uid VARCHAR(26)   NOT NULL,
    amount      NUMERIC(14,2) NOT NULL,
    currency    VARCHAR(3)    NOT NULL,
    note        VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_service_price_uid UNIQUE (uid)
);
-- Unique per (plan, kind, service). Postgres treats NULL plan_uid values as
-- distinct, but PaymentService also uses findCashPrice / upsert to avoid
-- creating duplicates for the cash price, so this is fine.
CREATE UNIQUE INDEX uk_md_service_price
    ON md_service_price(COALESCE(plan_uid, ''), kind, service_uid);
CREATE INDEX idx_md_service_price_service ON md_service_price(kind, service_uid);
CREATE INDEX idx_md_service_price_plan    ON md_service_price(plan_uid);
