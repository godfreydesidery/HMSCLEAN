-- ============================================================================
-- Nursing medication-administration record / MAR (PROCESS_MISMATCHES.md M15):
-- each bedside dose a nurse gives against an admission's prescription. Distinct
-- from pharmacy dispense — captures dose given, route, response, time + nurse.
-- ============================================================================

CREATE TABLE medication_administration (
    id                      BIGSERIAL    PRIMARY KEY,
    uid                     VARCHAR(26)  NOT NULL,
    admission_uid           VARCHAR(26)  NOT NULL,
    prescription_uid        VARCHAR(26)  NOT NULL,
    patient_uid             VARCHAR(26)  NOT NULL,
    medicine_uid            VARCHAR(26),
    dose_given              VARCHAR(120) NOT NULL,
    route                   VARCHAR(80),
    patient_response        VARCHAR(500),
    notes                   VARCHAR(1000),
    administered_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    administered_by_username VARCHAR(64) NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by              VARCHAR(80),
    updated_by              VARCHAR(80),
    version                 BIGINT,
    CONSTRAINT uk_medication_administration_uid UNIQUE (uid)
);

CREATE INDEX idx_med_admin_admission    ON medication_administration(admission_uid);
CREATE INDEX idx_med_admin_prescription ON medication_administration(prescription_uid);
