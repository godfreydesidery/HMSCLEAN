-- ============================================================================
-- Consultation transfer (OPC-1): faithful two-phase clinic-to-clinic hand-off.
--
-- A sibling aggregate to consultation. The treating doctor raises a PENDING
-- transfer against a target CLINIC only (no clinician — chosen at pickup); the
-- source consultation flips to TRANSFERRED. Reception later picks it up,
-- choosing the receiving clinician, which COMPLETES the transfer and books a
-- fresh consultation at the target clinic. The initiating doctor may instead
-- CANCEL it, restoring the source to IN_PROGRESS.
-- ============================================================================

CREATE TABLE consultation_transfer (
    id                        BIGSERIAL PRIMARY KEY,
    uid                       VARCHAR(26)  NOT NULL,

    source_consultation_uid   VARCHAR(26)  NOT NULL,
    patient_uid               VARCHAR(26)  NOT NULL,
    target_clinic_uid         VARCHAR(26)  NOT NULL,
    reason                    VARCHAR(500),

    status                    VARCHAR(16)  NOT NULL,

    created_consultation_uid  VARCHAR(26),
    cancel_reason             VARCHAR(500),
    completed_at              TIMESTAMP WITH TIME ZONE,
    cancelled_at              TIMESTAMP WITH TIME ZONE,

    created_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by                VARCHAR(80),
    updated_by                VARCHAR(80),
    version                   BIGINT,
    CONSTRAINT uk_consultation_transfer_uid UNIQUE (uid)
);

CREATE INDEX idx_consultation_transfer_status  ON consultation_transfer(status);
CREATE INDEX idx_consultation_transfer_patient ON consultation_transfer(patient_uid);
