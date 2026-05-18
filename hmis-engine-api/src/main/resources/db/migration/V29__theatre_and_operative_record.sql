-- ============================================================================
-- Phase 24: theatre masterdata + procedure scheduling + structured
-- operative record (PROCESS.md §7, §17.6, §17.13).
--
--   - md_theatre              — masterdata entry per operating theatre.
--   - clinical_order columns  — theatre_uid + scheduled_at + scheduled_by
--                                for PROCEDURE-kind orders.
--   - operative_record        — structured op note (1:1 with the
--                                procedure order). Sibling of order_result.
-- ============================================================================

-- ----- Theatre masterdata -------------------------------------------------

CREATE TABLE md_theatre (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    location    VARCHAR(80),
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_theatre_uid  UNIQUE (uid),
    CONSTRAINT uk_md_theatre_code UNIQUE (code)
);

CREATE INDEX idx_md_theatre_active ON md_theatre(active);

-- Seed two sample theatres so the procedure-scheduling flow is testable
-- out of the box. Matches the seeded sample data convention from V4.
INSERT INTO md_theatre (uid, code, name, location, description, active,
                        created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000TH1', 'OT1', 'Main Theatre 1', 'Block A, Level 2',
        'General-purpose operating theatre',                TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000TH2', 'OT2', 'Theatre 2',      'Block A, Level 2',
        'Secondary theatre; minor procedures + day cases',  TRUE, NOW(), NOW(), 'system', 'system', 0);

-- ----- Clinical-order scheduling columns ----------------------------------

ALTER TABLE clinical_order ADD COLUMN theatre_uid           VARCHAR(26);
ALTER TABLE clinical_order ADD COLUMN scheduled_at          TIMESTAMP WITH TIME ZONE;
ALTER TABLE clinical_order ADD COLUMN scheduled_by_username VARCHAR(64);

CREATE INDEX idx_clinical_order_theatre   ON clinical_order(theatre_uid);
CREATE INDEX idx_clinical_order_scheduled ON clinical_order(scheduled_at);

-- ----- Structured operative record ----------------------------------------

CREATE TABLE operative_record (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26) NOT NULL,

    order_uid                VARCHAR(26) NOT NULL,

    findings                 VARCHAR(4000),
    technique                VARCHAR(4000),
    instruments              VARCHAR(2000),
    complications            VARCHAR(2000),
    specimens                VARCHAR(2000),

    surgeon_username         VARCHAR(64),
    assistants               VARCHAR(500),
    anaesthetist_username    VARCHAR(64),
    anaesthesia_type         VARCHAR(64),
    scrub_nurse              VARCHAR(120),
    circulating_nurse        VARCHAR(120),

    started_at               TIMESTAMP WITH TIME ZONE,
    ended_at                 TIMESTAMP WITH TIME ZONE,

    authored_by_username     VARCHAR(64) NOT NULL,
    authored_at              TIMESTAMP WITH TIME ZONE NOT NULL,

    locked_at                TIMESTAMP WITH TIME ZONE,
    locked_by_username       VARCHAR(64),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_operative_record_uid   UNIQUE (uid),
    CONSTRAINT uk_operative_record_order UNIQUE (order_uid)
);

CREATE INDEX idx_operative_record_started ON operative_record(started_at);
CREATE INDEX idx_operative_record_surgeon ON operative_record(surgeon_username);
