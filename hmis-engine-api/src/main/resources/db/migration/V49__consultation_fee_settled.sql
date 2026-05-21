-- ============================================================================
-- Restore the legacy consultation-fee gate (PROCESS_MISMATCHES.md M3/M10).
--
-- The doctor's "from reception" queue shows a consultation only once its
-- consultation fee is settled (legacy PAID/COVERED); opening is refused for a
-- CASH patient until then. Because the encounter module must not depend on
-- billing, payment status is denormalised onto the consultation via a local
-- flag set by the billing-side settlement dispatcher.
--
--   * fee_settled    — TRUE once the consultation fee is paid (CASH) or the
--                      invoice is zero-amount (follow-up / plan waiver).
--                      Non-CASH consultations are treated as settled by the
--                      queue/gate even when the flag is FALSE.
--   * fee_settled_at — when it was settled.
-- ============================================================================

ALTER TABLE consultation
    ADD COLUMN fee_settled    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN fee_settled_at TIMESTAMP WITH TIME ZONE;

-- Backfill: non-CASH consultations and any consultation whose CONSULTATION
-- invoice is already PAID (or zero balance) are settled.
UPDATE consultation c
   SET fee_settled    = TRUE,
       fee_settled_at = NOW()
 WHERE c.payment_type <> 'CASH'
    OR EXISTS (
         SELECT 1 FROM invoice i
          WHERE i.consultation_uid = c.uid
            AND i.scope = 'CONSULTATION'
            AND (i.status = 'PAID'
                 OR (i.subtotal - i.total_paid - i.total_credited) <= 0));

CREATE INDEX idx_consultation_fee_settled ON consultation(fee_settled);
