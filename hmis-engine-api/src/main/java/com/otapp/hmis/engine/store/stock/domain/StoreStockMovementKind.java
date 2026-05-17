package com.otapp.hmis.engine.store.stock.domain;

/**
 * Why a store stock balance moved. Drives reporting categories and audit display.
 *
 * <ul>
 *   <li>{@code RECEIPT}     — stock arrived from procurement (GRN).</li>
 *   <li>{@code ISSUE}       — stock left the store to fulfil a transfer (TO) to a pharmacy.</li>
 *   <li>{@code DIRECT_ISSUE}— consumables issued direct to a ward (no pharmacy intermediary).</li>
 *   <li>{@code ADJUSTMENT}  — manual correction (positive or negative).</li>
 *   <li>{@code WASTAGE}     — discarded due to expiry, breakage, etc.</li>
 *   <li>{@code RETURN}      — stock returned from a pharmacy back to store.</li>
 * </ul>
 */
public enum StoreStockMovementKind {
    RECEIPT,
    ISSUE,
    DIRECT_ISSUE,
    ADJUSTMENT,
    WASTAGE,
    RETURN
}
