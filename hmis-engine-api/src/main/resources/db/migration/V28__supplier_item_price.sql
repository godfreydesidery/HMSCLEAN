-- ============================================================================
-- Phase 23b: per-supplier item price list (PROCESS.md §10, §17.9).
--
-- One row per (supplier, medicine, validity window) quote. A supplier
-- accumulates many quotes over time — the rolled-forward "current best"
-- view is computed in the service via findActiveForMedicine ordered by
-- unit_price ASC. Soft on/off via the `active` flag is independent of
-- the date window so a quote can be parked without losing history.
-- ============================================================================

CREATE TABLE supplier_item_price (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)   NOT NULL,

    supplier_uid    VARCHAR(26)   NOT NULL,
    medicine_uid    VARCHAR(26)   NOT NULL,

    unit_price      NUMERIC(14,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL,

    valid_from      DATE          NOT NULL,
    valid_to        DATE,
    active          BOOLEAN       NOT NULL,

    notes           VARCHAR(500),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_supplier_item_price_uid     UNIQUE (uid),
    CONSTRAINT ck_supplier_item_price_unit    CHECK (unit_price > 0),
    CONSTRAINT ck_supplier_item_price_window  CHECK (valid_to IS NULL OR valid_to >= valid_from)
);

CREATE INDEX idx_supplier_item_price_supplier   ON supplier_item_price(supplier_uid);
CREATE INDEX idx_supplier_item_price_medicine   ON supplier_item_price(medicine_uid);
CREATE INDEX idx_supplier_item_price_lookup
    ON supplier_item_price(supplier_uid, medicine_uid, active, valid_from);
CREATE INDEX idx_supplier_item_price_valid_from ON supplier_item_price(valid_from);
