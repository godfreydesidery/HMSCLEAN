package com.otapp.hmis.engine.pharmacy.stock.domain;

/**
 * Why a stock balance moved. Drives reporting categories and audit display.
 *
 * <ul>
 *   <li>{@code RECEIPT}    — stock arrived (e.g. from procurement).</li>
 *   <li>{@code DISPENSE}   — stock left the shelf to fulfil a prescription.</li>
 *   <li>{@code ADJUSTMENT} — manual correction (positive or negative).</li>
 *   <li>{@code WASTAGE}    — discarded due to expiry, breakage, etc.</li>
 *   <li>{@code TRANSFER_IN} / {@code TRANSFER_OUT} — moved between pharmacies.</li>
 * </ul>
 */
public enum StockMovementKind {
    RECEIPT,
    DISPENSE,
    ADJUSTMENT,
    WASTAGE,
    TRANSFER_IN,
    TRANSFER_OUT
}
