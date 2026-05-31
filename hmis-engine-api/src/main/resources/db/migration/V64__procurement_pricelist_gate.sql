-- ============================================================================
-- Procurement price-list gate (additive, legacy-faithful).
--
-- (1) Supplier: VAT registration (vrn), contract terms, and bank-account
--     block — these print on the LPO / cheque / remittance documents in the
--     legacy app (com.orbix.api.domain.Supplier). All nullable.
-- (2) GoodsReceiptLine: manufactured date captured alongside the existing
--     batch_no + expires_at, mirroring the legacy GRN detail batch.
-- (3) StoreStockBatch: manufactured date so the captured value flows into
--     stock when the GRN is approved.
--
-- The PO add-line supplier-quoted gate + contracted-price pull is a pure
-- service-layer change against the existing supplier_item_price table (V28)
-- and purchase_order_line.unit_cost (V15) — no schema change needed there.
-- ============================================================================

-- (1) Supplier additive columns ---------------------------------------------
ALTER TABLE supplier ADD COLUMN vrn                 VARCHAR(32);
ALTER TABLE supplier ADD COLUMN terms_of_contract   VARCHAR(1000);
ALTER TABLE supplier ADD COLUMN bank_name           VARCHAR(120);
ALTER TABLE supplier ADD COLUMN bank_account_name   VARCHAR(200);
ALTER TABLE supplier ADD COLUMN bank_account_no     VARCHAR(64);

-- (2) Goods-receipt line manufactured date ----------------------------------
ALTER TABLE goods_receipt_line ADD COLUMN manufactured_date DATE;

-- (3) Store stock batch manufactured date ------------------------------------
ALTER TABLE store_stock_batch ADD COLUMN manufactured_date DATE;
