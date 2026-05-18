package com.otapp.hmis.engine.transfer.pharmacystorereturn.domain;

/**
 * Lifecycle of a pharmacy-to-store return.
 *
 * <pre>
 *   DRAFT ──► SUBMITTED ──► COMPLETED
 *         \             \─► REJECTED
 *          └────────────────► CANCELLED
 * </pre>
 *
 * Stock effects fire only on the COMPLETED transition (pharmacy FEFO
 * TRANSFER_OUT, store RETURN credit). REJECTED is the store's "won't
 * take this back" decision; CANCELLED is the pharmacy walking it back
 * before the store has seen it.
 */
public enum PharmacyStoreReturnStatus {
    DRAFT,
    SUBMITTED,
    COMPLETED,
    REJECTED,
    CANCELLED
}
