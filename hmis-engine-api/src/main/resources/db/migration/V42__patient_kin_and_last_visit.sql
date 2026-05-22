-- ============================================================================
-- Phase 40: patient registry polish (PROCESS.md §17.1).
--
--   * Legacy supports up to 3 next-of-kin contacts; add kin2_* / kin3_*
--     columns alongside the existing kin_* set. Kept on the patient row
--     rather than a child table — almost always 0..3 short fields, no
--     benefit from normalisation.
--   * last_visit_at is denormalised onto patient so the registry list
--     can sort by recency without joining encounter tables. Set by
--     ConsultationService.book and AdmissionService.admit.
-- ============================================================================

ALTER TABLE patient
    ADD COLUMN kin2_full_name    VARCHAR(160),
    ADD COLUMN kin2_relationship VARCHAR(80),
    ADD COLUMN kin2_phone_no     VARCHAR(40),
    ADD COLUMN kin3_full_name    VARCHAR(160),
    ADD COLUMN kin3_relationship VARCHAR(80),
    ADD COLUMN kin3_phone_no     VARCHAR(40),
    ADD COLUMN last_visit_at     TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_patient_last_visit_at ON patient(last_visit_at);
