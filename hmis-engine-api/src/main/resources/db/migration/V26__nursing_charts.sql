-- ============================================================================
-- Nursing-side chart tables (PROCESS.md §4, §17.14 item 8):
--   1. admission_vitals_entry   — continuous observation chart (vitals series)
--   2. nursing_care_plan_item   — problem / goal / intervention / evaluation,
--                                  ACTIVE → RESOLVED / CANCELLED
--   3. dressing_chart_entry     — wound assessment + dressing record series
--
-- Consumable chart deferred — depends on a ward → pharmacy issue path
-- that doesn't exist yet.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Observation chart (continuous vitals)
-- ----------------------------------------------------------------------------

CREATE TABLE admission_vitals_entry (
    id                     BIGSERIAL PRIMARY KEY,
    uid                    VARCHAR(26) NOT NULL,

    admission_uid          VARCHAR(26) NOT NULL,
    recorded_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_by_username   VARCHAR(64) NOT NULL,

    temperature_c          NUMERIC(4,1),
    pulse_bpm              INTEGER,
    respirations_bpm       INTEGER,
    systolic_bp            INTEGER,
    diastolic_bp           INTEGER,
    spo2_percent           INTEGER,
    blood_glucose_mmol     NUMERIC(5,2),
    pain_score             INTEGER,
    notes                  VARCHAR(500),

    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by             VARCHAR(80),
    updated_by             VARCHAR(80),
    version                BIGINT,
    CONSTRAINT uk_admission_vitals_entry_uid UNIQUE (uid),
    CONSTRAINT ck_admission_vitals_pain      CHECK (pain_score IS NULL OR pain_score BETWEEN 0 AND 10),
    CONSTRAINT ck_admission_vitals_spo2      CHECK (spo2_percent IS NULL OR spo2_percent BETWEEN 0 AND 100)
);

CREATE INDEX idx_admission_vitals_admission ON admission_vitals_entry(admission_uid, recorded_at);
CREATE INDEX idx_admission_vitals_recorded  ON admission_vitals_entry(recorded_at);

-- ----------------------------------------------------------------------------
-- Nursing care plan
-- ----------------------------------------------------------------------------

CREATE TABLE nursing_care_plan_item (
    id                     BIGSERIAL PRIMARY KEY,
    uid                    VARCHAR(26) NOT NULL,

    admission_uid          VARCHAR(26) NOT NULL,
    problem                VARCHAR(500)  NOT NULL,
    goal                   VARCHAR(500)  NOT NULL,
    intervention           VARCHAR(2000) NOT NULL,
    evaluation             VARCHAR(2000),

    status                 VARCHAR(16) NOT NULL,
    opened_by_username     VARCHAR(64) NOT NULL,
    opened_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_by_username     VARCHAR(64),
    closed_at              TIMESTAMP WITH TIME ZONE,
    close_reason           VARCHAR(255),

    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by             VARCHAR(80),
    updated_by             VARCHAR(80),
    version                BIGINT,
    CONSTRAINT uk_nursing_care_plan_item_uid UNIQUE (uid)
);

CREATE INDEX idx_nursing_care_admission ON nursing_care_plan_item(admission_uid, opened_at);
CREATE INDEX idx_nursing_care_status    ON nursing_care_plan_item(status);

-- ----------------------------------------------------------------------------
-- Dressing chart
-- ----------------------------------------------------------------------------

CREATE TABLE dressing_chart_entry (
    id                     BIGSERIAL PRIMARY KEY,
    uid                    VARCHAR(26) NOT NULL,

    admission_uid          VARCHAR(26) NOT NULL,
    recorded_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_by_username   VARCHAR(64) NOT NULL,

    wound_location         VARCHAR(160) NOT NULL,
    wound_status           VARCHAR(16)  NOT NULL,
    dressing_applied       VARCHAR(500) NOT NULL,
    notes                  VARCHAR(1000),

    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by             VARCHAR(80),
    updated_by             VARCHAR(80),
    version                BIGINT,
    CONSTRAINT uk_dressing_chart_entry_uid UNIQUE (uid)
);

CREATE INDEX idx_dressing_admission ON dressing_chart_entry(admission_uid, recorded_at);
CREATE INDEX idx_dressing_status    ON dressing_chart_entry(wound_status);
