-- ============================================================================
-- Phase 37: wastage write-offs + multi-pharmacy dispense
-- (PROCESS.md §17.7 closing items).
--
-- Adds:
--   * pharmacy_stock_movement.wastage_reason — structured reason on the
--     WASTAGE row so the shrinkage report can categorise losses (EXPIRED,
--     DAMAGED, RECALLED, LOST, OTHER). Nullable; only set on WASTAGE rows.
--   * prescription.issue_pharmacy_uid / sales_pharmacy_uid — the legacy
--     "filled at A, stock pulled from B" split. Both populated at dispense
--     time; equal in the common case.
--   * pharmacy_sale_order_line.issue_pharmacy_uid / sales_pharmacy_uid —
--     same idea for OTC sale lines.
-- ============================================================================

ALTER TABLE pharmacy_stock_movement
    ADD COLUMN wastage_reason VARCHAR(16);

ALTER TABLE prescription
    ADD COLUMN issue_pharmacy_uid VARCHAR(26),
    ADD COLUMN sales_pharmacy_uid VARCHAR(26);

ALTER TABLE pharmacy_sale_order_line
    ADD COLUMN issue_pharmacy_uid VARCHAR(26),
    ADD COLUMN sales_pharmacy_uid VARCHAR(26);

-- Indices so cross-pharmacy reporting ("what did pharmacy X sell on behalf
-- of pharmacy Y") doesn't full-scan.
CREATE INDEX idx_prescription_sales_pharmacy
    ON prescription(sales_pharmacy_uid)
    WHERE sales_pharmacy_uid IS NOT NULL;

CREATE INDEX idx_sale_line_sales_pharmacy
    ON pharmacy_sale_order_line(sales_pharmacy_uid)
    WHERE sales_pharmacy_uid IS NOT NULL;
