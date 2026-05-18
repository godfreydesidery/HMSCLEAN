package com.otapp.hmis.engine.encounter.discharge.domain;

/**
 * Lifecycle of a structured discharge plan (PROCESS.md §3.3).
 *
 * <pre>
 *   PENDING ──► APPROVED
 *         \─►  CANCELLED
 * </pre>
 *
 * APPROVED is the terminal "ready" state — at that point the underlying
 * admission is closed via {@link com.otapp.hmis.engine.encounter.admission.domain.Admission}.
 * Once APPROVED the plan is immutable.
 */
public enum DischargePlanStatus {
    PENDING,
    APPROVED,
    CANCELLED
}
