-- ============================================================================
-- Phase 36: invoice scope discriminator + registration fee
-- (PROCESS.md §17.1 + §17.10).
--
-- An invoice can be raised against a consultation, an admission, or directly
-- against a patient (OUTSIDER walk-up + REGISTRATION). Registration and
-- outsider-clinical invoices share the same "consultation_uid IS NULL AND
-- admission_uid IS NULL" shape, so a queryable scope column is required to
-- keep findDraftOutsiderForPatient from returning a registration invoice and
-- wiping its lines on regeneration.
-- ============================================================================

ALTER TABLE invoice ADD COLUMN scope VARCHAR(16);

UPDATE invoice SET scope =
    CASE
        WHEN consultation_uid IS NOT NULL THEN 'CONSULTATION'
        WHEN admission_uid    IS NOT NULL THEN 'ADMISSION'
        ELSE 'OUTSIDER'
    END;

ALTER TABLE invoice ALTER COLUMN scope SET NOT NULL;

-- Cheap composite index for the registration-fee lookups (one row per patient).
CREATE INDEX idx_invoice_patient_scope ON invoice(patient_uid, scope);

-- ----- OUTSIDER pathway DB constraints --------------------------------------
-- The Java entities ClinicalOrder.consultationUid + Prescription.consultationUid
-- have been nullable for OUTSIDER (walk-in) flows since the OUTSIDER plumbing
-- landed, but the original V9 schema constraints were never relaxed.
-- InvoiceService.generateForOutsider's findAllByPatientUidAndConsultationUidIsNull*
-- queries can therefore never see anything in practice. Drop the constraints.
ALTER TABLE clinical_order ALTER COLUMN consultation_uid DROP NOT NULL;
ALTER TABLE prescription    ALTER COLUMN consultation_uid DROP NOT NULL;

-- ----- Registration fee cash price ------------------------------------------
-- ServiceKind.REGISTRATION uses the sentinel service_uid 'DEFAULT'. Insurance
-- plans can override or waive (amount = 0) by inserting a plan-specific row
-- — RegistrationFeeListeners.onConsultationBookingRequested ignores
-- zero-balance invoices, so a plan waiver does not gate the consultation.
INSERT INTO md_service_price (uid, plan_uid, kind, service_uid, amount, currency, note,
                              created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000RG1', NULL, 'REGISTRATION', 'DEFAULT', 5000.00, 'TZS',
     'Default cash registration fee',
     NOW(), NOW(), 'system', 'system', 0);
