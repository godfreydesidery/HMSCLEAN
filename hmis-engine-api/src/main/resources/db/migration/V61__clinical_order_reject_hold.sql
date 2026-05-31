-- ============================================================================
-- Clinical-order REJECT / HOLD lifecycle (legacy Zana-HMIS fidelity).
--
-- Legacy lab/radiology technicians could REJECT an order with a reason (a
-- recoverable bounce-back, re-accepted later) and HOLD an accepted order
-- (paused back to the pending queue, stamping who held it). The rewrite had
-- only REQUESTED/ACCEPTED/.../CANCELLED. This adds the reject/hold audit
-- columns. REJECTED is stored in the existing `status` enum column (TEXT), so
-- no type change is needed. Purely additive — all columns nullable, no backfill.
-- ============================================================================

ALTER TABLE clinical_order
    ADD COLUMN rejected_at          TIMESTAMP WITH TIME ZONE,
    ADD COLUMN rejected_by_username VARCHAR(64),
    ADD COLUMN reject_reason        VARCHAR(255),
    ADD COLUMN held_at              TIMESTAMP WITH TIME ZONE,
    ADD COLUMN held_by_username     VARCHAR(64);
