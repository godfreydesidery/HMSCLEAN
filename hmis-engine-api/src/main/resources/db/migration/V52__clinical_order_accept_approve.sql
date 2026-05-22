-- ============================================================================
-- Restore the legacy clinical-order gates (PROCESS_MISMATCHES.md M14/M16):
--   ACCEPTED (lab/radiology) / APPROVED (procedure sign-off) before work begins.
-- Adds the audit timestamps for the new gate transitions. The status column
-- already stores the value as text, so the new ACCEPTED/APPROVED enum values
-- need no schema change.
-- ============================================================================

ALTER TABLE clinical_order
    ADD COLUMN accepted_at          TIMESTAMP WITH TIME ZONE,
    ADD COLUMN approved_at          TIMESTAMP WITH TIME ZONE,
    ADD COLUMN approved_by_username VARCHAR(64);
