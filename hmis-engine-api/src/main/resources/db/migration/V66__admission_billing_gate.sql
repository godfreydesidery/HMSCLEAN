-- ============================================================================
-- Admission bill-clearance gate (legacy PatientResource.get_discharge_summary /
-- get_referral_summary / get_deceased_summary).
--
-- Legacy blocked SIGNED-OUT (discharge / referral / deceased closure) while any
-- of the admission's PatientBills was UNPAID or VERIFIED. Because the encounter
-- module must NOT depend on billing (modulith: encounter -> {common, iam,
-- masterdata, patient}; billing depends ON encounter), the gate is enforced from
-- a local denormalised flag on the admission that the billing-side settlement
-- dispatcher maintains in the allowed billing -> encounter direction — exactly
-- the pattern used for consultation.fee_settled (V49).
--
--   * bills_cleared    — TRUE when the admission has no outstanding bill. Billing
--                        clears it to FALSE when an admission invoice is issued
--                        with a positive balance (or a refund / partial credit
--                        re-opens one) and sets it back to TRUE when the invoice
--                        is fully settled (PAID / zero balance). Default TRUE: an
--                        admission with no bill is trivially clear, as in legacy.
--   * bills_cleared_at — when the admission was last marked clear.
--
-- AdmissionStatus / BedStatus enums are unchanged (no AWAITING_PAYMENT /
-- payment-gated bed occupancy redesign — that is a deliberate BY_DESIGN
-- simplification and out of scope for this additive gate).
-- ============================================================================

ALTER TABLE admission
    ADD COLUMN bills_cleared    BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN bills_cleared_at TIMESTAMP WITH TIME ZONE;

-- Re-arm the gate for any in-flight admission that currently has an outstanding
-- (issued / partially-paid, positive-balance) admission invoice. Pre-existing
-- admissions with no outstanding bill keep the TRUE default (they were
-- dischargeable before this gate existed and must remain so).
UPDATE admission a
   SET bills_cleared    = FALSE,
       bills_cleared_at = NULL
 WHERE EXISTS (
         SELECT 1 FROM invoice i
          WHERE i.admission_uid = a.uid
            AND i.scope  = 'ADMISSION'
            AND i.status IN ('ISSUED', 'PARTIALLY_PAID')
            AND (i.subtotal - i.total_paid - i.total_credited) > 0);

-- O(1) outstanding-bill check (AdmissionBillGate.existsOutstandingForAdmission):
-- a partial index over only the live, billable admission invoices.
CREATE INDEX idx_invoice_admission_outstanding
    ON invoice(admission_uid)
 WHERE scope = 'ADMISSION'
   AND status IN ('ISSUED', 'PARTIALLY_PAID');
