package com.otapp.hmis.engine.billing.cashshift.domain;

/**
 * Lifecycle of a cashier shift.
 *
 * <pre>
 *   OPEN ──► CLOSED
 * </pre>
 *
 * On CLOSED the service snapshots the declared float, computes the
 * expected (opening + cash payments received in window) and stores
 * the variance for end-of-day audit.
 */
public enum CashierShiftStatus {
    OPEN,
    CLOSED
}
