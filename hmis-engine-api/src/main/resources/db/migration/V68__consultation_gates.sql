-- ============================================================================
-- Consultation gates (legacy do_consultation / open_consultation /
-- cancel_consultation / free_consultation / change_type / change_payment_type).
--
-- These guards are mostly behavioural and reuse existing tables, so the schema
-- delta is minimal:
--
--   * consultation.signed_out_at — the legacy SIGNED-OUT moment, stamped on
--     sign-out (free_consultation). Distinct from the generic completed_at so
--     audit can tell a sign-out from a transfer-close. Nullable.
--
--   * Composite indexes for the sign-out downstream sweep — the cascade scans
--     a consultation's orders / prescriptions by (consultation_uid, status).
--
--   * invoice(consultation_uid) — the billing-side reversal looks the
--     consultation invoice up by consultation_uid.
--
-- No new tables. The cancel reversal reuses the existing invoice / credit_note
-- / refund tables; the IN_PROGRESS write-gate and the type/payment guards are
-- pure application rules. All money stays NUMERIC(14,2).
-- ============================================================================

ALTER TABLE consultation
    ADD COLUMN signed_out_at TIMESTAMP WITH TIME ZONE;

-- Backfill: a completed consultation in the rewrite is a legacy sign-out.
UPDATE consultation
   SET signed_out_at = completed_at
 WHERE status = 'COMPLETED'
   AND completed_at IS NOT NULL;

-- Sign-out downstream sweep — scan a consultation's orders / Rx by status.
CREATE INDEX idx_clinical_order_consultation_status ON clinical_order(consultation_uid, status);
CREATE INDEX idx_prescription_consultation_status   ON prescription(consultation_uid, status);

-- Billing-side reversal looks up the consultation invoice by consultation_uid.
CREATE INDEX idx_invoice_consultation ON invoice(consultation_uid);
