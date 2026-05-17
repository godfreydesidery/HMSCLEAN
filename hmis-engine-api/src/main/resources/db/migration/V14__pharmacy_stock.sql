-- ============================================================================
-- Pharmacy module: per-pharmacy stock balances + movement ledger.
-- ============================================================================

CREATE TABLE pharmacy_stock_balance (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    pharmacy_uid    VARCHAR(26)  NOT NULL,
    medicine_uid    VARCHAR(26)  NOT NULL,
    quantity        INTEGER      NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_stock_balance_uid               UNIQUE (uid),
    CONSTRAINT uk_stock_balance_pharmacy_medicine UNIQUE (pharmacy_uid, medicine_uid),
    CONSTRAINT ck_stock_balance_non_negative     CHECK (quantity >= 0)
);

CREATE INDEX idx_stock_balance_pharmacy ON pharmacy_stock_balance(pharmacy_uid);
CREATE INDEX idx_stock_balance_medicine ON pharmacy_stock_balance(medicine_uid);

CREATE TABLE pharmacy_stock_movement (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    pharmacy_uid    VARCHAR(26)  NOT NULL,
    medicine_uid    VARCHAR(26)  NOT NULL,
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
    CONSTRAINT uk_stock_movement_uid UNIQUE (uid)
);

CREATE INDEX idx_stock_movement_pharmacy  ON pharmacy_stock_movement(pharmacy_uid, occurred_at);
CREATE INDEX idx_stock_movement_medicine  ON pharmacy_stock_movement(medicine_uid);
CREATE INDEX idx_stock_movement_kind      ON pharmacy_stock_movement(kind);
CREATE INDEX idx_stock_movement_reference ON pharmacy_stock_movement(reference_uid);
