package com.otapp.hmis.engine.billing.invoice.domain;

/**
 * <pre>
 *   DRAFT ──► ISSUED ──► PARTIALLY_PAID ──► PAID
 *       \           \
 *        └─► CANCELLED
 * </pre>
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PARTIALLY_PAID,
    PAID,
    CANCELLED
}
