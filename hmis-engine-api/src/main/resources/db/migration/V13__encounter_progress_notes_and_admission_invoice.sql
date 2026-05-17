-- ============================================================================
-- Encounter module: progress notes recorded during an admission.
-- Billing module: extend invoice so it can be raised against an admission
-- (in addition to a consultation).
-- ============================================================================

CREATE TABLE admission_progress_note (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    admission_uid   VARCHAR(26)  NOT NULL,
    kind            VARCHAR(16)  NOT NULL,
    author_username VARCHAR(64)  NOT NULL,
    recorded_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    body            VARCHAR(8000) NOT NULL,

    deleted_at      TIMESTAMP WITH TIME ZONE,
    deleted_by      VARCHAR(80),
    deleted_reason  VARCHAR(255),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_progress_note_uid UNIQUE (uid)
);

CREATE INDEX idx_progress_note_admission ON admission_progress_note(admission_uid, recorded_at);
CREATE INDEX idx_progress_note_kind      ON admission_progress_note(kind);

-- Invoices can now also be raised against an admission. consultation_uid stays
-- optional; an invoice belongs to exactly one of (consultation, admission).
ALTER TABLE invoice DROP CONSTRAINT uk_invoice_consultation;
ALTER TABLE invoice ALTER COLUMN consultation_uid DROP NOT NULL;
ALTER TABLE invoice ADD COLUMN admission_uid VARCHAR(26);

CREATE UNIQUE INDEX uk_invoice_consultation ON invoice(consultation_uid) WHERE consultation_uid IS NOT NULL;
CREATE UNIQUE INDEX uk_invoice_admission    ON invoice(admission_uid)    WHERE admission_uid    IS NOT NULL;
