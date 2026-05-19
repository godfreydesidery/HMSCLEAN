package com.otapp.hmis.engine.encounter.consumable.domain;

/**
 * Kinds of {@link ConsumableStockMovement} rows.
 *
 * <ul>
 *   <li>{@code RECEIPT}      — store/pharmacy receives consumables from procurement.</li>
 *   <li>{@code ISSUE_TO_WARD} — issued to an admission via {@link ConsumableIssue}. Negative.</li>
 *   <li>{@code ADJUSTMENT}   — signed stock-take correction.</li>
 *   <li>{@code WASTAGE}      — write-off (expired, damaged, lost). Negative.</li>
 * </ul>
 */
public enum ConsumableMovementKind {
    RECEIPT,
    ISSUE_TO_WARD,
    ADJUSTMENT,
    WASTAGE
}
