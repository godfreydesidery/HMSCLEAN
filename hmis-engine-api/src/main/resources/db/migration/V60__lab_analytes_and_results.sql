-- ============================================================================
-- Structured lab results + reference ranges (PROCESS.md §5 / §17.4 fidelity gap).
--
-- Inherits the legacy process (a lab test is a panel of analytes; each result
-- value is measured against the applicable reference range and abnormals are
-- flagged) WITHOUT the legacy design flaw (legacy LabTestTypeRange was a
-- name-only string + a manual Low/Med/High pick). Here ranges carry real
-- numeric bounds, are banded by sex + age, and flagging is computed
-- deterministically server-side.
--
--   md_lab_test_analyte   — the ordered, typed analytes of a lab test type
--   md_lab_reference_range — sex/age-banded numeric reference range per analyte
--   lab_result_line       — per-analyte measured value on an OrderResult, with
--                            the applicable range SNAPSHOTTED + a computed flag
--
-- The generic order_result (narrative + impression) is unchanged; lines are
-- additive and only populated for LAB_TEST orders, so radiology/procedure
-- narrative results are untouched. Parent links use the parent's ULID `uid`
-- (loose coupling, same convention as md_medicine_unit / order_result).
-- ============================================================================

-- ----- Analyte catalogue (masterdata) --------------------------------------
CREATE TABLE md_lab_test_analyte (
    id                 BIGSERIAL PRIMARY KEY,
    uid                VARCHAR(26)  NOT NULL,
    lab_test_type_uid  VARCHAR(26)  NOT NULL,        -- parent panel (md_lab_test_type.uid)
    code               VARCHAR(32)  NOT NULL,        -- analyte mnemonic, e.g. WBC / HGB / PLT
    name               VARCHAR(120) NOT NULL,
    unit               VARCHAR(32),                  -- per-analyte unit discipline the legacy lacked
    value_kind         VARCHAR(16)  NOT NULL,        -- NUMERIC | TEXT
    display_order      INTEGER      NOT NULL DEFAULT 0,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         VARCHAR(80),
    updated_by         VARCHAR(80),
    version            BIGINT,
    CONSTRAINT uk_md_lab_test_analyte_uid       UNIQUE (uid),
    CONSTRAINT uk_md_lab_test_analyte_type_code UNIQUE (lab_test_type_uid, code)
);
CREATE INDEX idx_md_lab_test_analyte_type ON md_lab_test_analyte(lab_test_type_uid);

-- ----- Sex/age-banded reference ranges (masterdata) ------------------------
CREATE TABLE md_lab_reference_range (
    id            BIGSERIAL PRIMARY KEY,
    uid           VARCHAR(26)  NOT NULL,
    analyte_uid   VARCHAR(26)  NOT NULL,             -- parent analyte (md_lab_test_analyte.uid)
    sex           VARCHAR(8)   NOT NULL,             -- ANY | MALE | FEMALE
    age_min_days  INTEGER,                           -- NULL = open-ended lower bound
    age_max_days  INTEGER,                           -- NULL = open-ended upper bound
    ref_low       NUMERIC(14,4),
    ref_high      NUMERIC(14,4),
    critical_low  NUMERIC(14,4),
    critical_high NUMERIC(14,4),
    normal_text   VARCHAR(200),                      -- expected value for TEXT analytes (e.g. "Negative")
    range_display VARCHAR(120),                      -- human label, e.g. "4.0 - 11.0"
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(80),
    updated_by    VARCHAR(80),
    version       BIGINT,
    CONSTRAINT uk_md_lab_reference_range_uid UNIQUE (uid),
    CONSTRAINT ck_md_lab_reference_range_age CHECK (age_min_days IS NULL OR age_max_days IS NULL OR age_min_days <= age_max_days)
);
CREATE INDEX idx_md_lab_reference_range_analyte ON md_lab_reference_range(analyte_uid);

-- ----- Structured result lines (encounter) ---------------------------------
CREATE TABLE lab_result_line (
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(26)  NOT NULL,
    order_result_uid    VARCHAR(26)  NOT NULL,       -- parent result (order_result.uid)
    analyte_uid         VARCHAR(26)  NOT NULL,       -- masterdata analyte this line measured
    -- snapshot of the analyte definition at entry time -----------------------
    analyte_code        VARCHAR(32)  NOT NULL,
    analyte_name        VARCHAR(120) NOT NULL,
    value_kind          VARCHAR(16)  NOT NULL,
    unit                VARCHAR(32),
    -- the measured value -----------------------------------------------------
    value_numeric       NUMERIC(14,4),
    value_text          VARCHAR(2000),
    flag                VARCHAR(16)  NOT NULL,       -- NONE|NORMAL|LOW|HIGH|CRITICAL_LOW|CRITICAL_HIGH|ABNORMAL
    -- snapshot of the applicable reference range at entry time ----------------
    ref_low             NUMERIC(14,4),
    ref_high            NUMERIC(14,4),
    critical_low        NUMERIC(14,4),
    critical_high       NUMERIC(14,4),
    range_normal_text   VARCHAR(200),
    range_display       VARCHAR(120),
    reference_range_uid VARCHAR(26),                 -- audit pointer to the masterdata range applied (may be null)
    display_order       INTEGER      NOT NULL DEFAULT 0,
    note                VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(80),
    updated_by          VARCHAR(80),
    version             BIGINT,
    CONSTRAINT uk_lab_result_line_uid           UNIQUE (uid),
    CONSTRAINT uk_lab_result_line_result_analyte UNIQUE (order_result_uid, analyte_uid)
);
CREATE INDEX idx_lab_result_line_result ON lab_result_line(order_result_uid);

-- ============================================================================
-- Seed analytes + ranges for the lab test types seeded in V4 (CBC, FBG) so a
-- fresh DB can demonstrate structured results immediately. Seed uids are
-- readable fixed 26-char strings (not real ULIDs — only runtime rows are).
-- ============================================================================

-- CBC panel (md_lab_test_type uid 01J5KQRPCD0000000000000LB1): WBC, HGB, PLT
INSERT INTO md_lab_test_analyte (uid, lab_test_type_uid, code, name, unit, value_kind, display_order, active,
                                 created_at, updated_at, created_by, updated_by, version) VALUES
    ('LABANALYTE0000000000000WBC', '01J5KQRPCD0000000000000LB1', 'WBC', 'White Blood Cells', '10^9/L', 'NUMERIC', 1, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('LABANALYTE0000000000000HGB', '01J5KQRPCD0000000000000LB1', 'HGB', 'Haemoglobin',        'g/dL',   'NUMERIC', 2, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('LABANALYTE0000000000000PLT', '01J5KQRPCD0000000000000LB1', 'PLT', 'Platelets',          '10^9/L', 'NUMERIC', 3, TRUE, NOW(), NOW(), 'system', 'system', 0);

-- FBG single-analyte test (md_lab_test_type uid 01J5KQRPCD0000000000000LB3)
INSERT INTO md_lab_test_analyte (uid, lab_test_type_uid, code, name, unit, value_kind, display_order, active,
                                 created_at, updated_at, created_by, updated_by, version) VALUES
    ('LABANALYTE0000000000000FBG', '01J5KQRPCD0000000000000LB3', 'FBG', 'Fasting Blood Glucose', 'mg/dL', 'NUMERIC', 1, TRUE, NOW(), NOW(), 'system', 'system', 0);

-- Reference ranges (adult, sex-banded where clinically relevant)
INSERT INTO md_lab_reference_range (uid, analyte_uid, sex, age_min_days, age_max_days, ref_low, ref_high,
                                    critical_low, critical_high, normal_text, range_display, active,
                                    created_at, updated_at, created_by, updated_by, version) VALUES
    ('LABREFRANGE000000000000WB1', 'LABANALYTE0000000000000WBC', 'ANY',    NULL, NULL, 4.0,   11.0,  1.0,  30.0,   NULL, '4.0 - 11.0',  TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('LABREFRANGE000000000000HGM', 'LABANALYTE0000000000000HGB', 'MALE',   NULL, NULL, 13.0,  17.0,  7.0,  20.0,   NULL, '13.0 - 17.0', TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('LABREFRANGE000000000000HGF', 'LABANALYTE0000000000000HGB', 'FEMALE', NULL, NULL, 12.0,  15.0,  7.0,  20.0,   NULL, '12.0 - 15.0', TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('LABREFRANGE000000000000PL1', 'LABANALYTE0000000000000PLT', 'ANY',    NULL, NULL, 150.0, 450.0, 20.0, 1000.0, NULL, '150 - 450',   TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('LABREFRANGE000000000000FB1', 'LABANALYTE0000000000000FBG', 'ANY',    NULL, NULL, 70.0,  100.0, 40.0, 500.0,  NULL, '70 - 100',    TRUE, NOW(), NOW(), 'system', 'system', 0);
