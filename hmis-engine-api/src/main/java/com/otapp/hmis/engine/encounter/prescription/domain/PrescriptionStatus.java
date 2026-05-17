package com.otapp.hmis.engine.encounter.prescription.domain;

/**
 * <pre>
 *   REQUESTED ──► DISPENSED
 *           \
 *            └─► CANCELLED
 * </pre>
 */
public enum PrescriptionStatus {
    REQUESTED,
    DISPENSED,
    CANCELLED
}
