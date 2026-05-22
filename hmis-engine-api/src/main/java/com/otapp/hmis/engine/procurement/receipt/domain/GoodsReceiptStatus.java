package com.otapp.hmis.engine.procurement.receipt.domain;

/**
 * Lifecycle of a goods receipt note (PROCESS.md §10, §17.9).
 *
 * <pre>
 *   PENDING ─► VERIFIED ─► APPROVED
 *         \        \
 *          └────────┴──► REJECTED
 * </pre>
 *
 * <p>A GRN is created PENDING — no stock is touched yet. The store-keeper
 * verifies the physical count, then a separate approver signs off; the
 * APPROVED transition is what drives the store stock increment and the
 * PO line's recordReceipt. REJECTED is terminal with no stock impact
 * (e.g. wrong goods or count mismatch that can't be reconciled).
 */
public enum GoodsReceiptStatus {
    PENDING,
    VERIFIED,
    APPROVED,
    REJECTED
}
