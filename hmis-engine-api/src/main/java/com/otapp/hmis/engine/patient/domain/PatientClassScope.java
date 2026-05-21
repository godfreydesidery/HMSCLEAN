package com.otapp.hmis.engine.patient.domain;

/**
 * The patient-class lens used to scope role work queues, mirroring the legacy
 * outpatient / inpatient / outsider list split.
 *
 * <p>Note the asymmetry with {@link PatientType}: OUTPATIENT and OUTSIDER are
 * patient-type values, but INPATIENT is not — "inpatient" is realised by an
 * active admission. Queues therefore resolve this scope as:
 * <ul>
 *   <li><b>OUTSIDER</b> — work raised directly on the patient
 *       ({@code consultation_uid IS NULL}).</li>
 *   <li><b>OUTPATIENT</b> — consultation-bound work for a patient with no
 *       active admission.</li>
 *   <li><b>INPATIENT</b> — work for a patient who currently has an ADMITTED
 *       admission, regardless of their stored {@link PatientType}.</li>
 * </ul>
 */
public enum PatientClassScope {
    OUTPATIENT,
    INPATIENT,
    OUTSIDER
}
