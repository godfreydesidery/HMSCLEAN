package com.otapp.hmis.engine.procurement.supplierinvoice.domain;

/**
 * Lifecycle of a supplier invoice (PROCESS.md §17.9 three-way match).
 *
 * <pre>
 *   DRAFT ─► SUBMITTED ─► APPROVED ─► PAID
 *        \           \─► REJECTED
 *         └────────────► CANCELLED   (from DRAFT only)
 * </pre>
 *
 * APPROVED runs the three-way match — invoiced ≤ received ≤ ordered
 * per line. Cumulative across all approved invoices on the same PO.
 */
public enum SupplierInvoiceStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    PAID,
    REJECTED,
    CANCELLED
}
