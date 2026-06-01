-- Two more nursing-side chart series the legacy PatientNursingChart carried but
-- the rewrite had dropped (gap audit ADMIT-1 / ADMIT-2):
--   1. fluid_balance_entry  — intake / output (urine, drainage) monitoring; the
--      ICU/HDU fluid-balance chart. Net = intake − (urine + drainage).
--   2. care_activity_entry  — the per-shift care log (feeding, repositioning,
--      bed bath) + bedside blood-sugar readings (random / fasting).
-- Both follow the existing admission-scoped, immutable, append-only chart pattern
-- (admission_vitals_entry / dressing_chart_entry).

-- ----------------------------------------------------------------------------
-- Fluid-balance chart (intake / output)
-- ----------------------------------------------------------------------------

CREATE TABLE fluid_balance_entry (
    id                     BIGSERIAL PRIMARY KEY,
    uid                    VARCHAR(26) NOT NULL,

    admission_uid          VARCHAR(26) NOT NULL,
    recorded_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_by_username   VARCHAR(64) NOT NULL,

    intake_ml              INTEGER,
    urine_output_ml        INTEGER,
    drainage_output_ml     INTEGER,
    notes                  VARCHAR(500),

    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by             VARCHAR(80),
    updated_by             VARCHAR(80),
    version                BIGINT,
    CONSTRAINT uk_fluid_balance_entry_uid UNIQUE (uid),
    CONSTRAINT ck_fluid_balance_intake    CHECK (intake_ml          IS NULL OR intake_ml          >= 0),
    CONSTRAINT ck_fluid_balance_urine     CHECK (urine_output_ml    IS NULL OR urine_output_ml    >= 0),
    CONSTRAINT ck_fluid_balance_drainage  CHECK (drainage_output_ml IS NULL OR drainage_output_ml >= 0)
);

CREATE INDEX idx_fluid_balance_admission ON fluid_balance_entry(admission_uid, recorded_at);
CREATE INDEX idx_fluid_balance_recorded  ON fluid_balance_entry(recorded_at);

-- ----------------------------------------------------------------------------
-- Care-activity chart (per-shift nursing tasks + bedside blood sugar)
-- ----------------------------------------------------------------------------

CREATE TABLE care_activity_entry (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26) NOT NULL,

    admission_uid            VARCHAR(26) NOT NULL,
    recorded_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_by_username     VARCHAR(64) NOT NULL,

    feeding_done             BOOLEAN NOT NULL DEFAULT FALSE,
    position_changed         BOOLEAN NOT NULL DEFAULT FALSE,
    bed_bath_done            BOOLEAN NOT NULL DEFAULT FALSE,
    random_blood_sugar_mmol  NUMERIC(5,2),
    fasting_blood_sugar_mmol NUMERIC(5,2),
    notes                    VARCHAR(500),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_care_activity_entry_uid UNIQUE (uid)
);

CREATE INDEX idx_care_activity_admission ON care_activity_entry(admission_uid, recorded_at);
