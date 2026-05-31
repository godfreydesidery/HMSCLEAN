-- ============================================================================
-- Insurer-covered settlement bucket on the invoice.
--
-- A COVERED invoice line is BILLED on the patient's invoice (it shows on the
-- bill) but PAID by the patient's insurance plan up front — legacy Zana-HMIS
-- landed such a bill at balance 0, settled by the scheme. Without a place to
-- record the insurer payment, the covered amount stayed in the subtotal as
-- patient balance and double-charged the patient.
--
-- total_covered mirrors total_paid (cash) and total_credited (write-downs) as a
-- distinct, auditable settlement channel. The patient balance nets all three
-- out: balance = subtotal - total_paid - total_credited - total_covered.
-- ============================================================================

ALTER TABLE invoice
    ADD COLUMN total_covered NUMERIC(14,2) NOT NULL DEFAULT 0;
