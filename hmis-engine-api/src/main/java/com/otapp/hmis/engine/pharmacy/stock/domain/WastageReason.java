package com.otapp.hmis.engine.pharmacy.stock.domain;

/**
 * Why a batch was written off. Captured on the WASTAGE
 * {@link StockMovement} so end-of-day stock-loss reports can categorise
 * shrinkage rather than treat every write-off as opaque.
 */
public enum WastageReason {
    EXPIRED,
    DAMAGED,
    RECALLED,
    LOST,
    OTHER
}
