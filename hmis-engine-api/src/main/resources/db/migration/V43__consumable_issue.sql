-- ============================================================================
-- Phase 41: patient consumable chart (PROCESS.md §17.3).
--
-- Records a single ward consumable (gauze, IV fluid bag, syringe, dressing
-- kit, etc.) issued against an inpatient admission. The unit_cost is a
-- snapshot at issue-time; the admission invoice generator picks these
-- rows up as CONSUMABLE-kind lines.
--
-- The actual stock decrement at the source (store / pharmacy) is a
-- separate concern handled outside this aggregate.
-- ============================================================================

CREATE TABLE consumable_issue (
    id                    BIGSERIAL PRIMARY KEY,
    uid                   VARCHAR(26)  NOT NULL,

    admission_uid         VARCHAR(26)  NOT NULL,
    consumable_uid        VARCHAR(26)  NOT NULL,
    source_kind           VARCHAR(16)  NOT NULL,
    source_location_uid   VARCHAR(26)  NOT NULL,
    quantity              INTEGER      NOT NULL,
    unit_cost             NUMERIC(14,2) NOT NULL,
    issued_by_username    VARCHAR(64)  NOT NULL,
    issued_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    note                  VARCHAR(500),

    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by            VARCHAR(80),
    updated_by            VARCHAR(80),
    version               BIGINT,
    CONSTRAINT uk_consumable_issue_uid     UNIQUE (uid),
    CONSTRAINT ck_consumable_issue_qty     CHECK (quantity > 0),
    CONSTRAINT ck_consumable_issue_cost    CHECK (unit_cost >= 0)
);

CREATE INDEX idx_consumable_issue_admission  ON consumable_issue(admission_uid, issued_at);
CREATE INDEX idx_consumable_issue_consumable ON consumable_issue(consumable_uid);

-- ----- Sample consumables ---------------------------------------------------
-- Two common nursing consumables seeded so the UI / tests have stable UIDs
-- to work with. More can be added via /masterdata/consumables.
INSERT INTO md_consumable (uid, code, name, unit_of_measure, description, active,
                           created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000CB1', 'GAUZE', 'Sterile gauze swab', 'pack',
     'Standard 10x10 cm sterile gauze swabs (5 pcs/pack)', TRUE,
     NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000CB2', 'NSALINE', 'Normal saline 500ml', 'bag',
     'IV-grade 0.9% sodium chloride, 500 ml bag', TRUE,
     NOW(), NOW(), 'system', 'system', 0);

