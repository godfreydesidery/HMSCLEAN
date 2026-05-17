-- ============================================================================
-- Encounter module: consultations + patient vitals.
-- ============================================================================

-- Sequence powering CN-YYYY-NNNNNN consultation numbers.
CREATE SEQUENCE consultation_no_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE consultation (
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(26)  NOT NULL,
    consultation_no     VARCHAR(32)  NOT NULL,

    patient_uid         VARCHAR(26)  NOT NULL,
    clinic_uid          VARCHAR(26)  NOT NULL,
    clinician_username  VARCHAR(64)  NOT NULL,

    status              VARCHAR(16)  NOT NULL,
    payment_type        VARCHAR(16)  NOT NULL,
    insurance_plan_uid  VARCHAR(26),

    reason              VARCHAR(500),
    booked_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    cancel_reason       VARCHAR(255),

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(80),
    updated_by          VARCHAR(80),
    version             BIGINT,
    CONSTRAINT uk_consultation_uid UNIQUE (uid),
    CONSTRAINT uk_consultation_no  UNIQUE (consultation_no)
);

CREATE INDEX idx_consultation_patient   ON consultation(patient_uid);
CREATE INDEX idx_consultation_clinician ON consultation(clinician_username);
CREATE INDEX idx_consultation_status    ON consultation(status);
CREATE INDEX idx_consultation_started   ON consultation(started_at);

CREATE TABLE patient_vitals (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)  NOT NULL,

    consultation_uid         VARCHAR(26)  NOT NULL,
    patient_uid              VARCHAR(26)  NOT NULL,

    taken_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    temperature_c            NUMERIC(4,1),
    pulse_bpm                INTEGER,
    respiration_bpm          INTEGER,
    blood_pressure_sys       INTEGER,
    blood_pressure_dia       INTEGER,
    spo2_percent             INTEGER,
    weight_kg                NUMERIC(5,1),
    height_cm                NUMERIC(5,1),
    notes                    VARCHAR(500),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_patient_vitals_uid UNIQUE (uid)
);

CREATE INDEX idx_patient_vitals_consultation ON patient_vitals(consultation_uid);
CREATE INDEX idx_patient_vitals_patient      ON patient_vitals(patient_uid);
