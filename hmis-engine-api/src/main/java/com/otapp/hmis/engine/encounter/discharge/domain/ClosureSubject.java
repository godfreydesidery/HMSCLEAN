package com.otapp.hmis.engine.encounter.discharge.domain;

/**
 * What a closure plan ({@link DischargePlan}) closes:
 *
 * <ul>
 *   <li>{@code ADMISSION} — an inpatient stay; closure drives the admission to a
 *       terminal {@code AdmissionStatus} (DISCHARGED / DECEASED / TRANSFERRED).
 *       All three {@link DischargePlanKind}s are valid.</li>
 *   <li>{@code CONSULTATION} — an outpatient encounter; closure drives the
 *       consultation to DECEASED / REFERRED. Only DECEASED and REFERRAL kinds are
 *       valid — an outpatient is never "discharged home".</li>
 * </ul>
 *
 * Exactly one of {@code admissionUid} / {@code consultationUid} is set on a plan.
 */
public enum ClosureSubject {
    ADMISSION,
    CONSULTATION
}
