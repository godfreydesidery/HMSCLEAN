-- ============================================================================
-- Role-scoped lab/radiology/procedure queues (PROCESS_MISMATCHES.md M8).
--
-- Denormalised payment flag on the clinical order, set by the billing
-- settlement dispatcher when the invoice carrying the order's line is paid.
-- Surfaced on the role worklists; the encounter module never reads billing.
-- ============================================================================

ALTER TABLE clinical_order
    ADD COLUMN settled    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN settled_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_clinical_order_settled ON clinical_order(kind, status, settled);
