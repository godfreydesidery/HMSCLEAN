-- ============================================================================
-- Provider profile: optional clinical-identity sidecar for a user (1:1).
-- Restores the legacy Clinician.type (specialty) + registration / licence
-- without widening iam_user with columns that only apply to clinical staff.
-- ============================================================================

CREATE TABLE iam_provider_profile (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26) NOT NULL,
    user_uid        VARCHAR(26) NOT NULL,
    specialty       VARCHAR(64),
    registration_no VARCHAR(64),
    license_no      VARCHAR(64),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_iam_provider_profile_uid  UNIQUE (uid),
    CONSTRAINT uk_iam_provider_profile_user UNIQUE (user_uid)
);

CREATE INDEX idx_iam_provider_profile_user ON iam_provider_profile(user_uid);
