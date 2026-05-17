-- ============================================================================
-- Encounter module: inpatient admissions.
-- ============================================================================

-- Sequence powering AD-YYYY-NNNNNN admission numbers.
CREATE SEQUENCE admission_no_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE admission (
    id                              BIGSERIAL PRIMARY KEY,
    uid                             VARCHAR(26)  NOT NULL,
    admission_no                    VARCHAR(32)  NOT NULL,

    patient_uid                     VARCHAR(26)  NOT NULL,
    ward_uid                        VARCHAR(26)  NOT NULL,
    bed_label                       VARCHAR(32),

    admitting_clinician_username    VARCHAR(64)  NOT NULL,

    status                          VARCHAR(16)  NOT NULL,
    payment_type                    VARCHAR(16)  NOT NULL,
    insurance_plan_uid              VARCHAR(26),
    consultation_uid                VARCHAR(26),

    admission_reason                VARCHAR(500),
    admitted_at                     TIMESTAMP WITH TIME ZONE NOT NULL,
    discharged_at                   TIMESTAMP WITH TIME ZONE,
    discharge_summary               VARCHAR(1000),
    cancelled_at                    TIMESTAMP WITH TIME ZONE,
    cancel_reason                   VARCHAR(255),

    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by                      VARCHAR(80),
    updated_by                      VARCHAR(80),
    version                         BIGINT,
    CONSTRAINT uk_admission_uid UNIQUE (uid),
    CONSTRAINT uk_admission_no  UNIQUE (admission_no)
);

CREATE INDEX idx_admission_patient  ON admission(patient_uid);
CREATE INDEX idx_admission_ward     ON admission(ward_uid);
CREATE INDEX idx_admission_status   ON admission(status);
CREATE INDEX idx_admission_admitted ON admission(admitted_at);
