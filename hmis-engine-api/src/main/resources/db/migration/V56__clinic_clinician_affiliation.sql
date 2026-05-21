-- ============================================================================
-- Clinician ⇄ Clinic affiliation
-- Restores the legacy Clinician.clinics many-to-many: a clinician works at one
-- or more clinics, and a consultation may only be booked for a clinician who is
-- affiliated with the chosen clinic. Coupling to iam is by identity string
-- (canonical user_uid + denormalized username) — no cross-module FK.
-- ============================================================================

CREATE TABLE md_clinic_clinician (
    id         BIGSERIAL PRIMARY KEY,
    uid        VARCHAR(26) NOT NULL,
    clinic_uid VARCHAR(26) NOT NULL,
    user_uid   VARCHAR(26) NOT NULL,
    username   VARCHAR(64) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(80),
    updated_by VARCHAR(80),
    version    BIGINT,
    CONSTRAINT uk_md_clinic_clinician_uid UNIQUE (uid),
    CONSTRAINT uk_md_clinic_clinician     UNIQUE (clinic_uid, user_uid)
);

CREATE INDEX idx_md_clinic_clinician_clinic ON md_clinic_clinician(clinic_uid) WHERE active;
CREATE INDEX idx_md_clinic_clinician_user   ON md_clinic_clinician(user_uid);
