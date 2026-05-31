-- ============================================================================
-- Insurance coverage routing (faithful port of legacy Zana-HMIS charge accrual).
--
-- (A) Per-service COVERED flag on the pricing matrix. Mirrors the legacy
--     <Kind>InsurancePlan.covered boolean (LabTestTypeInsurancePlan etc.):
--     a plan price ROW EXISTING is NOT the same as the service being covered.
--     Only meaningful when plan_uid IS NOT NULL (cash rows ignore it). Default
--     FALSE, matching legacy covered=false default.
--
-- (B) Per-LINE payer routing on the invoice line. Copies legacy PatientBill
--     status semantics at charge time:
--       COVERED  = insurer pays in full (settled, on the insurer claim),
--       VERIFIED = insured-but-uncovered inpatient accrual (hospital fronts,
--                  patient/self-pay still owes),
--       UNPAID   = cash, owed.
--     membership_no is stamped on the line when COVERED (legacy
--     PatientBill.membershipNo); payer_plan_uid records the plan that covered
--     the line (null = self-pay / cash). principal_line_uid points a
--     supplementary ward top-up line at its COVERED principal (replaces the
--     legacy principal/supplementary self-FK with loose uid coupling).
-- ============================================================================

-- (A) ServicePrice.covered — rides the existing idx_md_service_price_plan index.
ALTER TABLE md_service_price
    ADD COLUMN covered BOOLEAN NOT NULL DEFAULT FALSE;

-- (B) InvoiceLine payer-routing columns.
ALTER TABLE invoice_line
    ADD COLUMN coverage_status   VARCHAR(16) NOT NULL DEFAULT 'UNPAID',
    ADD COLUMN membership_no     VARCHAR(64),
    ADD COLUMN payer_plan_uid    VARCHAR(26),
    ADD COLUMN principal_line_uid VARCHAR(26);

-- Look up supplementary top-up lines by their principal.
CREATE INDEX idx_invoice_line_principal ON invoice_line(principal_line_uid);
