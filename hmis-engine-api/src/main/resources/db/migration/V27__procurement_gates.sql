-- ============================================================================
-- Phase 23a: legacy-aligned approval gates on procurement documents
-- (PROCESS.md §10, §17.9).
--
--   PurchaseOrder: DRAFT -> VERIFIED -> APPROVED -> ORDERED -> ...,
--                  with REJECTED branch from pre-submission states.
--
--   GoodsReceipt:  PENDING -> VERIFIED -> APPROVED (stock credit fires
--                  here), with REJECTED branch — no stock impact.
-- ============================================================================

-- ----- Purchase order: workflow timestamp + reason columns ------------------

ALTER TABLE purchase_order ADD COLUMN verified_at    TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_order ADD COLUMN approved_at    TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_order ADD COLUMN rejected_at    TIMESTAMP WITH TIME ZONE;
ALTER TABLE purchase_order ADD COLUMN reject_reason  VARCHAR(255);

-- ----- Goods receipt: status + workflow gates ------------------------------

ALTER TABLE goods_receipt ADD COLUMN status                VARCHAR(16) NOT NULL DEFAULT 'PENDING';
ALTER TABLE goods_receipt ADD COLUMN verified_at           TIMESTAMP WITH TIME ZONE;
ALTER TABLE goods_receipt ADD COLUMN verified_by_username  VARCHAR(64);
ALTER TABLE goods_receipt ADD COLUMN approved_at           TIMESTAMP WITH TIME ZONE;
ALTER TABLE goods_receipt ADD COLUMN approved_by_username  VARCHAR(64);
ALTER TABLE goods_receipt ADD COLUMN rejected_at           TIMESTAMP WITH TIME ZONE;
ALTER TABLE goods_receipt ADD COLUMN rejected_by_username  VARCHAR(64);
ALTER TABLE goods_receipt ADD COLUMN reject_reason         VARCHAR(255);

-- Default only useful for migration of any pre-existing rows; remove so the
-- application-layer constructor remains the single source of truth.
ALTER TABLE goods_receipt ALTER COLUMN status DROP DEFAULT;

CREATE INDEX idx_goods_receipt_status ON goods_receipt(status);
