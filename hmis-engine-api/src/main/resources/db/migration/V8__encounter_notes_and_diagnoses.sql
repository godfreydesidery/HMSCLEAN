-- ============================================================================
-- Clinical notes (one per consultation) and consultation diagnoses
-- (working / final, many per consultation).
-- ============================================================================

CREATE TABLE clinical_note (
    id                              BIGSERIAL PRIMARY KEY,
    uid                             VARCHAR(26)  NOT NULL,
    consultation_uid                VARCHAR(26)  NOT NULL,
    chief_complaint                 VARCHAR(1000),
    history_of_presenting_illness   VARCHAR(4000),
    past_medical_history            VARCHAR(4000),
    examination                     VARCHAR(4000),
    assessment                      VARCHAR(4000),
    plan                            VARCHAR(4000),
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by                      VARCHAR(80),
    updated_by                      VARCHAR(80),
    version                         BIGINT,
    CONSTRAINT uk_clinical_note_uid          UNIQUE (uid),
    CONSTRAINT uk_clinical_note_consultation UNIQUE (consultation_uid)
);

CREATE TABLE consultation_diagnosis (
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(26)  NOT NULL,
    consultation_uid    VARCHAR(26)  NOT NULL,
    diagnosis_type_uid  VARCHAR(26)  NOT NULL,
    kind                VARCHAR(16)  NOT NULL,
    is_primary          BOOLEAN      NOT NULL DEFAULT FALSE,
    notes               VARCHAR(1000),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(80),
    updated_by          VARCHAR(80),
    version             BIGINT,
    CONSTRAINT uk_consultation_diagnosis_uid   UNIQUE (uid),
    CONSTRAINT uk_consultation_diagnosis_entry UNIQUE (consultation_uid, kind, diagnosis_type_uid)
);

CREATE INDEX idx_consultation_diagnosis_consultation ON consultation_diagnosis(consultation_uid);
CREATE INDEX idx_consultation_diagnosis_kind         ON consultation_diagnosis(kind);
