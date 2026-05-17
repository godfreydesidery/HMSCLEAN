-- ============================================================================
-- Master data: remaining 8 catalogs
--   md_ward, md_pharmacy, md_store, md_diagnosis_type, md_procedure_type,
--   md_lab_test_type, md_radiology_type, md_medicine, md_insurance_provider
--
-- All tables follow the same convention: surrogate BIGINT id (internal),
-- ULID `uid` (public), auditable timestamps + version, optional `active` flag.
-- ============================================================================

-- ----- Wards ---------------------------------------------------------------
CREATE TABLE md_ward (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    category    VARCHAR(32)  NOT NULL,
    capacity    INTEGER      NOT NULL,
    location    VARCHAR(80),
    description VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_ward_code UNIQUE (code),
    CONSTRAINT uk_md_ward_uid  UNIQUE (uid)
);
CREATE INDEX idx_md_ward_active   ON md_ward(active);
CREATE INDEX idx_md_ward_category ON md_ward(category);

-- ----- Pharmacies ----------------------------------------------------------
CREATE TABLE md_pharmacy (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    location    VARCHAR(80),
    description VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_pharmacy_code UNIQUE (code),
    CONSTRAINT uk_md_pharmacy_uid  UNIQUE (uid)
);
CREATE INDEX idx_md_pharmacy_active ON md_pharmacy(active);

-- ----- Stores --------------------------------------------------------------
CREATE TABLE md_store (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    location    VARCHAR(80),
    description VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_store_code UNIQUE (code),
    CONSTRAINT uk_md_store_uid  UNIQUE (uid)
);
CREATE INDEX idx_md_store_active ON md_store(active);

-- ----- Diagnosis types -----------------------------------------------------
CREATE TABLE md_diagnosis_type (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_diagnosis_type_code UNIQUE (code),
    CONSTRAINT uk_md_diagnosis_type_uid  UNIQUE (uid)
);
CREATE INDEX idx_md_diagnosis_type_active ON md_diagnosis_type(active);

-- ----- Procedure types -----------------------------------------------------
CREATE TABLE md_procedure_type (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_procedure_type_code UNIQUE (code),
    CONSTRAINT uk_md_procedure_type_uid  UNIQUE (uid)
);
CREATE INDEX idx_md_procedure_type_active ON md_procedure_type(active);

-- ----- Lab test types ------------------------------------------------------
CREATE TABLE md_lab_test_type (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    specimen    VARCHAR(80),
    unit        VARCHAR(32),
    description VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_lab_test_type_code UNIQUE (code),
    CONSTRAINT uk_md_lab_test_type_uid  UNIQUE (uid)
);
CREATE INDEX idx_md_lab_test_type_active ON md_lab_test_type(active);

-- ----- Radiology types -----------------------------------------------------
CREATE TABLE md_radiology_type (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    modality    VARCHAR(32)  NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_radiology_type_code UNIQUE (code),
    CONSTRAINT uk_md_radiology_type_uid  UNIQUE (uid)
);
CREATE INDEX idx_md_radiology_type_active   ON md_radiology_type(active);
CREATE INDEX idx_md_radiology_type_modality ON md_radiology_type(modality);

-- ----- Medicines -----------------------------------------------------------
CREATE TABLE md_medicine (
    id           BIGSERIAL PRIMARY KEY,
    uid          VARCHAR(26)  NOT NULL,
    code         VARCHAR(32)  NOT NULL,
    name         VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200),
    strength     VARCHAR(80),
    form         VARCHAR(32)  NOT NULL,
    description  VARCHAR(500),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by   VARCHAR(80),
    updated_by   VARCHAR(80),
    version      BIGINT,
    CONSTRAINT uk_md_medicine_code UNIQUE (code),
    CONSTRAINT uk_md_medicine_uid  UNIQUE (uid)
);
CREATE INDEX idx_md_medicine_active ON md_medicine(active);
CREATE INDEX idx_md_medicine_form   ON md_medicine(form);

-- ----- Insurance providers -------------------------------------------------
CREATE TABLE md_insurance_provider (
    id             BIGSERIAL PRIMARY KEY,
    uid            VARCHAR(26)  NOT NULL,
    code           VARCHAR(32)  NOT NULL,
    name           VARCHAR(200) NOT NULL,
    contact_person VARCHAR(120),
    phone          VARCHAR(40),
    email          VARCHAR(120),
    address        VARCHAR(255),
    description    VARCHAR(500),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by     VARCHAR(80),
    updated_by     VARCHAR(80),
    version        BIGINT,
    CONSTRAINT uk_md_insurance_provider_code UNIQUE (code),
    CONSTRAINT uk_md_insurance_provider_uid  UNIQUE (uid)
);
CREATE INDEX idx_md_insurance_provider_active ON md_insurance_provider(active);

-- ============================================================================
-- Seed a few sample rows so the UI has something to render on first start.
-- Each row uses a stable hardcoded ULID so the IDs are deterministic.
-- ============================================================================

INSERT INTO md_ward (uid, code, name, category, capacity, location, description, active,
                     created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000WG1', 'GENW', 'General Ward',       'GENERAL',   24, 'Block A',  'Adult general ward',          TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000WG2', 'PEDW', 'Pediatric Ward',     'PEDIATRIC', 12, 'Block B',  'Children inpatient ward',     TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000WG3', 'ICU',  'Intensive Care Unit','ICU',        6, 'Block C',  'Critical care',               TRUE, NOW(), NOW(), 'system', 'system', 0);

INSERT INTO md_pharmacy (uid, code, name, location, description, active,
                         created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000PH1', 'MAIN', 'Main Pharmacy',      'Ground floor',   'Central dispensing point', TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PH2', 'OPD',  'OPD Pharmacy',       'Outpatient wing','Outpatient dispensing',    TRUE, NOW(), NOW(), 'system', 'system', 0);

INSERT INTO md_store (uid, code, name, location, description, active,
                      created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000ST1', 'MAIN', 'Main Store',     'Basement',     'Central inventory store',  TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000ST2', 'CONS', 'Consumable Store','Block A',     'Consumables and supplies', TRUE, NOW(), NOW(), 'system', 'system', 0);

INSERT INTO md_diagnosis_type (uid, code, name, description, active,
                               created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000DG1', 'A09',     'Diarrhoea and gastroenteritis',     'Infectious origin', TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DG2', 'B50',     'Plasmodium falciparum malaria',     NULL,                TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000DG3', 'J11',     'Influenza, virus not identified',   NULL,                TRUE, NOW(), NOW(), 'system', 'system', 0);

INSERT INTO md_procedure_type (uid, code, name, description, active,
                               created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000PC1', 'DRESS',   'Wound Dressing',          'Standard wound cleaning + dressing',  TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PC2', 'INJECT',  'Intramuscular Injection', 'IM administration of medication',     TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PC3', 'SUTURE',  'Suturing',                'Wound suturing',                       TRUE, NOW(), NOW(), 'system', 'system', 0);

INSERT INTO md_lab_test_type (uid, code, name, specimen, unit, description, active,
                              created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000LB1', 'CBC',     'Complete Blood Count',      'Whole blood', NULL,    NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000LB2', 'BS_MPS',  'Malaria Parasites (BS-MPS)','Blood',       NULL,    NULL, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000LB3', 'FBG',     'Fasting Blood Glucose',     'Serum',       'mg/dL', NULL, TRUE, NOW(), NOW(), 'system', 'system', 0);

INSERT INTO md_radiology_type (uid, code, name, modality, description, active,
                               created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000RD1', 'CXR',     'Chest X-Ray',             'X_RAY',      'PA + lateral',                  TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RD2', 'AUS',     'Abdominal Ultrasound',    'ULTRASOUND', 'Abdominal scan',                TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RD3', 'CT_HEAD', 'CT Scan - Head',          'CT_SCAN',    'Plain or contrast-enhanced',    TRUE, NOW(), NOW(), 'system', 'system', 0);

INSERT INTO md_medicine (uid, code, name, generic_name, strength, form, description, active,
                         created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000MD1', 'PAR500',  'Panadol',               'Paracetamol',                  '500mg', 'TABLET',    'Analgesic / antipyretic', TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000MD2', 'AMX500',  'Amoxil',                'Amoxicillin',                  '500mg', 'CAPSULE',   'Broad-spectrum antibiotic',TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000MD3', 'COART',   'Coartem',               'Artemether/Lumefantrine',      '20/120mg','TABLET',  'Anti-malarial',           TRUE, NOW(), NOW(), 'system', 'system', 0);

INSERT INTO md_insurance_provider (uid, code, name, contact_person, phone, email, address, description, active,
                                   created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000IN1', 'NHIF', 'National Health Insurance Fund', NULL, NULL, NULL, NULL, 'Statutory scheme', TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000IN2', 'AAR',  'AAR Insurance',                  NULL, NULL, NULL, NULL, 'Private scheme',   TRUE, NOW(), NOW(), 'system', 'system', 0);
