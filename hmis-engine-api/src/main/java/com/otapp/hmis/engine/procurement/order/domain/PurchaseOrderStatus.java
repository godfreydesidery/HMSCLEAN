package com.otapp.hmis.engine.procurement.order.domain;

/**
 * Lifecycle of a purchase order — aligned with the legacy gates
 * (PROCESS.md §10, §17.9): procurement officer verifies, manager
 * approves, only then is the order sent to the supplier.
 *
 * <pre>
 *   DRAFT ─► VERIFIED ─► APPROVED ─► ORDERED ─► PARTIALLY_RECEIVED ─► RECEIVED
 *      \         \           \
 *       └─────────┴───────────┴─► REJECTED
 *
 *   any non-RECEIVED ─► CANCELLED
 * </pre>
 *
 * <p>Lines can only be added/edited while DRAFT. ORDERED is what the
 * legacy calls SUBMITTED — the moment the order is sent to the supplier
 * and the goods-receipt path opens. Receiving any quantity transitions
 * the order to PARTIALLY_RECEIVED until every line is fully received.
 */
public enum PurchaseOrderStatus {
    DRAFT,
    VERIFIED,
    APPROVED,
    ORDERED,
    PARTIALLY_RECEIVED,
    RECEIVED,
    REJECTED,
    CANCELLED
}
