package com.otapp.hmis.engine.procurement.order.domain;

/**
 * Lifecycle of a purchase order.
 *
 * <pre>
 *   DRAFT ──► ORDERED ──► PARTIALLY_RECEIVED ──► RECEIVED
 *        \         \─► CANCELLED
 * </pre>
 *
 * <p>Lines can only be added/edited while DRAFT. ORDERED locks the lines
 * and opens the goods-receipt path; receiving any quantity transitions
 * the order to PARTIALLY_RECEIVED until every line is fully received.
 */
public enum PurchaseOrderStatus {
    DRAFT,
    ORDERED,
    PARTIALLY_RECEIVED,
    RECEIVED,
    CANCELLED
}
