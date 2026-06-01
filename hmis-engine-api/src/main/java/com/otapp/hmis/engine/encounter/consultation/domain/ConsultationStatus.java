package com.otapp.hmis.engine.encounter.consultation.domain;

/**
 * Lifecycle of a consultation.
 *
 * <pre>
 *   BOOKED ──► IN_PROGRESS ──► COMPLETED
 *        \         \      \──► DECEASED  (patient died during the encounter)
 *         \         \      \──► REFERRED  (referred out to an external facility)
 *         └─► CANCELLED      └─► TRANSFERRED (handed off to another clinic)
 * </pre>
 *
 * <p>DECEASED and REFERRED are terminal outpatient-closure states driven by an
 * approved closure plan (the consultation-keyed equivalent of an inpatient
 * discharge plan) — legacy DeceasedNote / ReferralPlan on a consultation.
 */
public enum ConsultationStatus {
    BOOKED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    /** Patient was handed off to another clinic / clinician; see {@code transferredToConsultationUid}. */
    TRANSFERRED,
    /** Patient died during the encounter; closed via an approved DECEASED closure plan. */
    DECEASED,
    /** Patient referred out to an external facility; closed via an approved REFERRAL closure plan. */
    REFERRED
}
