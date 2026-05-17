package com.otapp.hmis.engine.pharmacy.sale.domain;

/**
 * Lifecycle of the sale-order header. The interesting state machine lives
 * on each line ({@link PharmacySaleLineStatus}); the header is a thin
 * roll-up used to drive list views and reporting.
 *
 * <pre>
 *   ACTIVE ──► COMPLETED
 *         \─► CANCELLED
 * </pre>
 *
 * <ul>
 *   <li>{@code ACTIVE}    — open; the pharmacist is still processing lines.</li>
 *   <li>{@code COMPLETED} — every line reached SOLD / REJECTED / CANCELLED.</li>
 *   <li>{@code CANCELLED} — force-closed by the pharmacist (every still-open
 *       line moves to CANCELLED at the same time).</li>
 * </ul>
 */
public enum PharmacySaleOrderStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}
