package com.otapp.hmis.engine.encounter.nursingchart.domain;

/**
 * Status of one item on a patient's nursing care plan.
 *
 * <pre>
 *   ACTIVE ──► RESOLVED
 *         \─►  CANCELLED
 * </pre>
 *
 * The plan is a living document — items can be added at any point during
 * the admission, marked resolved as the issue clears, or cancelled if
 * the team decides it's no longer relevant.
 */
public enum NursingCarePlanStatus {
    ACTIVE,
    RESOLVED,
    CANCELLED
}
