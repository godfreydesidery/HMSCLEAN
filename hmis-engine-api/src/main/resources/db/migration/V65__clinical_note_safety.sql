-- ============================================================================
-- clinical-note-safety (encounter module: note + vitals + prescription)
--
-- Additive only. Three legacy capabilities layered onto the existing
-- consultation + prescription lifecycles:
--   1) clinical_note: three extra narrative fields (legacy ClinicalNote).
--      drugs_and_allergy_history is the allergy-safety field surfaced to the
--      prescriber before prescribing.
--   2) patient_vitals: BMI / BSA measured values (legacy GeneralExamination,
--      free-text Strings) stored here as NUMERIC per the no-float rule, plus a
--      free-text bmi_comment.
--   3) prescription: composite indexes backing the new duplicate-drug guard
--      and the two prescribing-alert read queries.
-- All columns nullable; no backfill (legacy fields were optional).
-- ============================================================================

-- 1) Clinical note narrative fields (legacy: drugsAndAllergyHistory,
--    familyAndSocialHistory, reviewOfOtherSystems).
ALTER TABLE clinical_note ADD COLUMN drugs_and_allergy_history VARCHAR(4000);
ALTER TABLE clinical_note ADD COLUMN family_and_social_history VARCHAR(4000);
ALTER TABLE clinical_note ADD COLUMN review_of_other_systems   VARCHAR(4000);

-- 2) Vitals BMI / BSA (legacy GeneralExamination bodyMassIndex /
--    bodyMassIndexComment / bodySurfaceArea). NUMERIC, never float.
ALTER TABLE patient_vitals ADD COLUMN bmi         NUMERIC(4,1);
ALTER TABLE patient_vitals ADD COLUMN bsa         NUMERIC(4,2);
ALTER TABLE patient_vitals ADD COLUMN bmi_comment VARCHAR(255);

-- 3) Prescribing-alert / duplicate-guard support indexes.
--    (patient_uid, medicine_uid, status) — same-medicine-this-month and
--    unfinished-course alert lookups (filtered by SOLD, ordered by approved_at).
CREATE INDEX idx_prescription_patient_medicine_status
    ON prescription (patient_uid, medicine_uid, status);
--    (consultation_uid, medicine_uid) — duplicate-drug-per-consultation guard.
CREATE INDEX idx_prescription_consultation_medicine
    ON prescription (consultation_uid, medicine_uid);
