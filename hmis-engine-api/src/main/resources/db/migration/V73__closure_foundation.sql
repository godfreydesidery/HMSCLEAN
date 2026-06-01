-- ============================================================================
-- Closure foundation (gap audit cluster 2: DISCH-5, DISCH-3, DISCH-4).
--
--   1. md_external_medical_provider — masterdata for referral target facilities
--      (legacy ExternalMedicalProvider). Referenced by closure plans of kind
--      REFERRAL via external_provider_uid (loose uid coupling; the facility name
--      is denormalised onto referral_facility).
--   2. discharge_plan generalised from admission-only to a unified CLOSURE plan
--      that keys off EITHER an admission OR a consultation (legacy DeceasedNote /
--      ReferralPlan each carried both FKs, exactly one set). This lets an
--      OUTPATIENT death / external referral be recorded the same way an inpatient
--      closure is, and gives cluster 3 a single closure worklist.
--   3. patient gains a deceased flag so a patient recorded dead (inpatient or
--      outpatient) can no longer be re-booked / re-admitted (legacy patient
--      type DECEASED).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. External medical provider (referral target) masterdata
-- ----------------------------------------------------------------------------

CREATE TABLE md_external_medical_provider (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,

    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    address     VARCHAR(255),
    telephone   VARCHAR(40),
    email       VARCHAR(120),
    fax         VARCHAR(40),
    website     VARCHAR(200),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_external_medical_provider_uid  UNIQUE (uid),
    CONSTRAINT uk_md_external_medical_provider_code UNIQUE (code)
);

CREATE INDEX idx_md_external_medical_provider_name ON md_external_medical_provider(name);

-- ----------------------------------------------------------------------------
-- 2. Generalise discharge_plan into a unified closure plan
-- ----------------------------------------------------------------------------

-- A plan now belongs to either an admission OR a consultation. subject_type
-- records which; backfill existing rows (all admission-bound) before adding the
-- NOT NULL.
ALTER TABLE discharge_plan ADD COLUMN subject_type         VARCHAR(16);
ALTER TABLE discharge_plan ADD COLUMN consultation_uid     VARCHAR(26);
ALTER TABLE discharge_plan ADD COLUMN external_provider_uid VARCHAR(26);

UPDATE discharge_plan SET subject_type = 'ADMISSION' WHERE subject_type IS NULL;
ALTER TABLE discharge_plan ALTER COLUMN subject_type SET NOT NULL;

-- admission_uid is no longer mandatory (consultation-subject plans have it NULL).
ALTER TABLE discharge_plan ALTER COLUMN admission_uid DROP NOT NULL;

-- Replace the plain UNIQUE(admission_uid) with a partial unique index so NULLs
-- (consultation-subject plans) don't collide, and add the symmetric one for
-- consultations. Exactly one of the two subject uids must be set.
ALTER TABLE discharge_plan DROP CONSTRAINT uk_discharge_plan_admission;
CREATE UNIQUE INDEX uk_discharge_plan_admission
    ON discharge_plan(admission_uid)    WHERE admission_uid    IS NOT NULL;
CREATE UNIQUE INDEX uk_discharge_plan_consultation
    ON discharge_plan(consultation_uid) WHERE consultation_uid IS NOT NULL;

ALTER TABLE discharge_plan
    ADD CONSTRAINT ck_discharge_plan_one_subject
    CHECK ( (admission_uid IS NOT NULL AND consultation_uid IS NULL)
         OR (admission_uid IS NULL     AND consultation_uid IS NOT NULL) );

CREATE INDEX idx_discharge_plan_subject ON discharge_plan(subject_type);

-- ----------------------------------------------------------------------------
-- 3. Patient deceased flag
-- ----------------------------------------------------------------------------

ALTER TABLE patient ADD COLUMN deceased    BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE patient ADD COLUMN deceased_at TIMESTAMP WITH TIME ZONE;
