-- ============================================================================
-- Phase 46: consumable stock (PROCESS.md §17.3 closing item — the
-- decrement-on-issue layer that was deliberately cut from Phase 41).
--
-- Integer balance per (sourceKind, sourceUid, consumableUid) — no batches
-- or expiry, matching how warehouses actually track gauze / saline /
-- dressings. ConsumableIssueService.issue calls
-- ConsumableStockService.decrementForIssue inside the same tx so the
-- audit row and the source decrement land together.
-- ============================================================================

CREATE TABLE consumable_stock_balance (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    source_kind     VARCHAR(16)  NOT NULL,
    source_uid      VARCHAR(26)  NOT NULL,
    consumable_uid  VARCHAR(26)  NOT NULL,
    quantity        INTEGER      NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_consumable_stock_balance_uid UNIQUE (uid),
    CONSTRAINT uk_consumable_stock_balance
        UNIQUE (source_kind, source_uid, consumable_uid),
    CONSTRAINT ck_consumable_stock_balance_qty CHECK (quantity >= 0)
);

CREATE INDEX idx_consumable_stock_balance_source
    ON consumable_stock_balance(source_kind, source_uid);

CREATE TABLE consumable_stock_movement (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    source_kind     VARCHAR(16)  NOT NULL,
    source_uid      VARCHAR(26)  NOT NULL,
    consumable_uid  VARCHAR(26)  NOT NULL,
    kind            VARCHAR(16)  NOT NULL,
    quantity        INTEGER      NOT NULL,
    balance_after   INTEGER      NOT NULL,
    reference_uid   VARCHAR(26),
    note            VARCHAR(500),
    occurred_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_username  VARCHAR(64),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_consumable_stock_movement_uid UNIQUE (uid)
);

CREATE INDEX idx_consumable_stock_movement_source
    ON consumable_stock_movement(source_kind, source_uid, occurred_at);
CREATE INDEX idx_consumable_stock_movement_consumable
    ON consumable_stock_movement(consumable_uid);
CREATE INDEX idx_consumable_stock_movement_reference
    ON consumable_stock_movement(reference_uid)
    WHERE reference_uid IS NOT NULL;
