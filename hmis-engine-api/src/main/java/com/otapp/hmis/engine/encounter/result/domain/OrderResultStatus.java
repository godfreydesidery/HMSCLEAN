package com.otapp.hmis.engine.encounter.result.domain;

/**
 * Lifecycle of a clinical order's result document.
 *
 * <pre>
 *   PRELIMINARY ──► FINAL ──► AMENDED (and back to AMENDED on further edits)
 * </pre>
 *
 * <p>PRELIMINARY means the result is being entered and may still change.
 * FINAL means it has been signed off — once final, the order itself is
 * marked COMPLETED and the impression is reflected on the order. AMENDED
 * is FINAL content that was later edited; downstream consumers should
 * surface the amendment timestamp.
 */
public enum OrderResultStatus {
    PRELIMINARY,
    FINAL,
    AMENDED
}
