package com.otapp.hmis.engine.encounter.admission.domain;

/**
 * Lifecycle of an inpatient admission.
 *
 * <pre>
 *   ADMITTED ──► DISCHARGED
 *           \─► DECEASED
 *           \─► TRANSFERRED   (transferred out to another facility)
 *           \─► CANCELLED     (admission entered in error)
 * </pre>
 */
public enum AdmissionStatus {
    ADMITTED,
    DISCHARGED,
    DECEASED,
    TRANSFERRED,
    CANCELLED
}
