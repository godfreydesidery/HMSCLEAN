package com.otapp.hmis.engine.encounter.order.domain;

/**
 * <pre>
 *   REQUESTED ──► IN_PROGRESS ──► COMPLETED
 *           \             \
 *            └─────► CANCELLED
 * </pre>
 */
public enum ClinicalOrderStatus {
    REQUESTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
