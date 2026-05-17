package com.otapp.hmis.engine.encounter.consultation.domain;

/**
 * Lifecycle of a consultation.
 *
 * <pre>
 *   BOOKED ──► IN_PROGRESS ──► COMPLETED
 *        \         \
 *         └─► CANCELLED
 * </pre>
 */
public enum ConsultationStatus {
    BOOKED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
