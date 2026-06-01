-- ============================================================================
-- OPC-3: faithful nurse-fill vitals lifecycle on patient_vitals.
--
-- Legacy Zana-HMIS PatientVital carried a two-actor status lifecycle:
--   EMPTY → PENDING → SUBMITTED → ARCHIVED
-- (nurse fills → nurse submits/locks → doctor consumes into the exam). The
-- rewrite had recorded vitals inline as a finished reading with no status;
-- this migration adds the lifecycle column + the submit / archive timestamps.
--
-- DEFAULT for EXISTING ROWS: SUBMITTED. Historical inline-recorded readings were
-- captured-and-final but never "consumed" in the new sense, so SUBMITTED keeps
-- them valid AND lets a doctor still consume (→ ARCHIVED) them. (ARCHIVED would
-- have pre-empted that acknowledgement; PENDING would wrongly re-open a locked,
-- finished reading.) New rows start EMPTY and are driven through the lifecycle
-- by the service.
-- ============================================================================

ALTER TABLE patient_vitals ADD COLUMN status       VARCHAR(16);
ALTER TABLE patient_vitals ADD COLUMN submitted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE patient_vitals ADD COLUMN archived_at  TIMESTAMP WITH TIME ZONE;

-- Backfill historical rows to SUBMITTED, then enforce NOT NULL.
UPDATE patient_vitals SET status = 'SUBMITTED' WHERE status IS NULL;
ALTER TABLE patient_vitals ALTER COLUMN status SET NOT NULL;

CREATE INDEX idx_patient_vitals_status ON patient_vitals(status);
