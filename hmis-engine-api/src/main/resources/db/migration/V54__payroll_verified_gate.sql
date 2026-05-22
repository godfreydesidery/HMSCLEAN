-- ============================================================================
-- Restore the legacy payroll two-step sign-off (PROCESS_MISMATCHES.md M19):
-- DRAFT → VERIFIED (manager) → APPROVED (director) → PAID. Adds the VERIFIED
-- audit columns. The status column stores the value as text, so the new
-- VERIFIED enum value needs no schema change.
-- ============================================================================

ALTER TABLE hr_payroll_period
    ADD COLUMN verified_at          TIMESTAMP WITH TIME ZONE,
    ADD COLUMN verified_by_username VARCHAR(64);
