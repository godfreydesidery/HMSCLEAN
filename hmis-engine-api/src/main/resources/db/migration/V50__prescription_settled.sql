-- ============================================================================
-- Pharmacy dispensing queue support (PROCESS_MISMATCHES.md M5).
--
-- Denormalised payment flag on the prescription, set by the billing settlement
-- dispatcher when the invoice carrying the prescription's MEDICINE line is paid.
-- Surfaced on the dispense worklist. Not a hard pre-dispense gate (medicines are
-- billed at point of dispense in this rewrite).
-- ============================================================================

ALTER TABLE prescription
    ADD COLUMN settled    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN settled_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_prescription_settled ON prescription(status, settled);
