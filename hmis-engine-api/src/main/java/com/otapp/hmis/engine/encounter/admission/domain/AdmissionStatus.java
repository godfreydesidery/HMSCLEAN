package com.otapp.hmis.engine.encounter.admission.domain;

/**
 * Lifecycle of an inpatient admission.
 *
 * <pre>
 *   AWAITING_DEPOSIT ──► ADMITTED        (deposit / ward-bed bill settled)
 *                   \──► CANCELLED       (left before paying the deposit)
 *   ADMITTED ──► DISCHARGED
 *           \─► DECEASED
 *           \─► TRANSFERRED   (transferred out to another facility)
 *           \─► CANCELLED     (admission entered in error)
 * </pre>
 *
 * <p>{@code AWAITING_DEPOSIT} mirrors the legacy {@code PENDING} admission +
 * {@code WAITING} bed holding state (Zana-HMIS {@code doAdmission}): a patient who owes
 * out-of-pocket for the bed (CASH or MIXED) is admitted but the bed is only RESERVED —
 * not OCCUPIED — until the ward-bed bill is settled, at which point billing flips it to
 * {@code ADMITTED} and the bed to OCCUPIED. Pure INSURANCE admissions start
 * {@code ADMITTED} (legacy's fully-covered branch). The insured-but-uncovered ward case
 * (legacy holds it pending) also starts ADMITTED for now, pending admission coverage
 * routing.
 */
public enum AdmissionStatus {
    /** Admitted but deposit-pending: ward-bed bill issued, bed RESERVED, not yet active. */
    AWAITING_DEPOSIT,
    ADMITTED,
    DISCHARGED,
    DECEASED,
    TRANSFERRED,
    CANCELLED;

    /**
     * In-flight admission states that hold the patient: a patient in any of these
     * cannot be admitted again or start a fresh consultation, and counts as an active
     * encounter. {@code AWAITING_DEPOSIT} is included because legacy's {@code PENDING}
     * admission reserves the slot just like an active one.
     */
    public static final java.util.Set<AdmissionStatus> ACTIVE =
            java.util.Set.of(AWAITING_DEPOSIT, ADMITTED);
}
