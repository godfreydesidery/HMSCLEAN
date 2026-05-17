-- ============================================================================
-- Clinical orders (lab / radiology / procedure) and prescriptions raised
-- from a consultation.
-- ============================================================================

CREATE SEQUENCE clinical_order_no_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE prescription_no_seq   START WITH 1 INCREMENT BY 1;

CREATE TABLE clinical_order (
    id                 BIGSERIAL PRIMARY KEY,
    uid                VARCHAR(26)  NOT NULL,
    order_no           VARCHAR(32)  NOT NULL,
    consultation_uid   VARCHAR(26)  NOT NULL,
    patient_uid        VARCHAR(26)  NOT NULL,
    kind               VARCHAR(16)  NOT NULL,
    service_uid        VARCHAR(26)  NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    urgency            VARCHAR(16)  NOT NULL,
    requested_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at       TIMESTAMP WITH TIME ZONE,
    instructions       VARCHAR(1000),
    result             VARCHAR(4000),
    cancel_reason      VARCHAR(255),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         VARCHAR(80),
    updated_by         VARCHAR(80),
    version            BIGINT,
    CONSTRAINT uk_clinical_order_uid UNIQUE (uid),
    CONSTRAINT uk_clinical_order_no  UNIQUE (order_no)
);

CREATE INDEX idx_clinical_order_consultation ON clinical_order(consultation_uid);
CREATE INDEX idx_clinical_order_status       ON clinical_order(status);
CREATE INDEX idx_clinical_order_kind         ON clinical_order(kind);
CREATE INDEX idx_clinical_order_service      ON clinical_order(kind, service_uid);

CREATE TABLE prescription (
    id                 BIGSERIAL PRIMARY KEY,
    uid                VARCHAR(26)  NOT NULL,
    prescription_no    VARCHAR(32)  NOT NULL,
    consultation_uid   VARCHAR(26)  NOT NULL,
    patient_uid        VARCHAR(26)  NOT NULL,
    medicine_uid       VARCHAR(26)  NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    dose               VARCHAR(80)  NOT NULL,
    frequency          VARCHAR(80)  NOT NULL,
    duration_days      INTEGER,
    quantity           INTEGER,
    instructions       VARCHAR(500),
    requested_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    dispensed_at       TIMESTAMP WITH TIME ZONE,
    cancel_reason      VARCHAR(255),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         VARCHAR(80),
    updated_by         VARCHAR(80),
    version            BIGINT,
    CONSTRAINT uk_prescription_uid UNIQUE (uid),
    CONSTRAINT uk_prescription_no  UNIQUE (prescription_no)
);

CREATE INDEX idx_prescription_consultation ON prescription(consultation_uid);
CREATE INDEX idx_prescription_status       ON prescription(status);
CREATE INDEX idx_prescription_patient      ON prescription(patient_uid);
