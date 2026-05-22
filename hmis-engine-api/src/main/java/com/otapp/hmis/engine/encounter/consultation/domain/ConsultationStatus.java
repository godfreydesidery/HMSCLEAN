package com.otapp.hmis.engine.encounter.consultation.domain;

/**
 * Lifecycle of a consultation.
 *
 * <pre>
 *   BOOKED ──► IN_PROGRESS ──► COMPLETED
 *        \         \
 *         └─► CANCELLED      └─► TRANSFERRED (handed off to another clinic)
 * </pre>
 */
public enum ConsultationStatus {
    BOOKED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    /** Patient was handed off to another clinic / clinician; see {@code transferredToConsultationUid}. */
    TRANSFERRED
}
