-- ============================================================================
-- Repurpose patient.patient_type from NEW/RETURNING/REFERRAL/STAFF/DEPENDENT
-- (demographic category) to OUTPATIENT/OUTSIDER (routing classification),
-- matching the proven Zana-HMIS process.
--
-- Every existing patient is assumed to have flowed through the consultation
-- pathway, so they become OUTPATIENT. The OUTSIDER pathway (walk-ins
-- bypassing consultation) is opt-in going forward.
-- ============================================================================

UPDATE patient
   SET patient_type = 'OUTPATIENT'
 WHERE patient_type NOT IN ('OUTPATIENT', 'OUTSIDER');
