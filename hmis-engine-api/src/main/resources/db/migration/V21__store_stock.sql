-- ============================================================================
-- Store domain: per-store balance + per-batch on-hand + movement ledger.
--
-- Mirrors the pharmacy_stock_* tables but for the central store(s). The
-- procurement GRN now lands here (see V22) instead of in a pharmacy; stock
-- flows out of the store to pharmacies via transfers (Phase 20).
-- ============================================================================

CREATE TABLE store_stock_balance (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    store_uid       VARCHAR(26)  NOT NULL,
    medicine_uid    VARCHAR(26)  NOT NULL,
    quantity        INTEGER      NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_store_stock_balance_uid              UNIQUE (uid),
    CONSTRAINT uk_store_stock_balance_store_medicine   UNIQUE (store_uid, medicine_uid),
    CONSTRAINT ck_store_stock_balance_non_negative     CHECK (quantity >= 0)
);

CREATE INDEX idx_store_stock_balance_store    ON store_stock_balance(store_uid);
CREATE INDEX idx_store_stock_balance_medicine ON store_stock_balance(medicine_uid);

CREATE TABLE store_stock_batch (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    store_uid       VARCHAR(26)  NOT NULL,
    medicine_uid    VARCHAR(26)  NOT NULL,
    batch_no        VARCHAR(64)  NOT NULL,
    expires_at      DATE,
    received_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    quantity        INTEGER      NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_store_stock_batch_uid                       UNIQUE (uid),
    CONSTRAINT uk_store_stock_batch_store_medicine_batchno    UNIQUE (store_uid, medicine_uid, batch_no),
    CONSTRAINT ck_store_stock_batch_non_negative              CHECK (quantity >= 0)
);

CREATE INDEX idx_store_stock_batch_store_medicine ON store_stock_batch(store_uid, medicine_uid);
CREATE INDEX idx_store_stock_batch_expires        ON store_stock_batch(expires_at);

CREATE TABLE store_stock_movement (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    store_uid       VARCHAR(26)  NOT NULL,
    medicine_uid    VARCHAR(26)  NOT NULL,
    kind            VARCHAR(16)  NOT NULL,
    quantity        INTEGER      NOT NULL,
    balance_after   INTEGER      NOT NULL,
    reference_uid   VARCHAR(26),
    batch_uid       VARCHAR(26),
    batch_no        VARCHAR(64),
    note            VARCHAR(500),
    occurred_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_username  VARCHAR(64),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_store_stock_movement_uid UNIQUE (uid)
);

CREATE INDEX idx_store_stock_movement_store     ON store_stock_movement(store_uid, occurred_at);
CREATE INDEX idx_store_stock_movement_medicine  ON store_stock_movement(medicine_uid);
CREATE INDEX idx_store_stock_movement_kind      ON store_stock_movement(kind);
CREATE INDEX idx_store_stock_movement_reference ON store_stock_movement(reference_uid);
CREATE INDEX idx_store_stock_movement_batch     ON store_stock_movement(batch_uid);

-- ----------------------------------------------------------------------------
-- IAM: STORE_ACCESS privilege guards the new /store/** endpoints. Granted to
-- ROOT (gets everything anyway) and STORE_PERSON (the actual storekeeper role).
-- PROCUREMENT also needs it to record GRNs against store stock.
-- ----------------------------------------------------------------------------

INSERT INTO iam_privilege (uid, name, description, created_at, updated_at, created_by, updated_by, version)
VALUES ('01J5KQRPCD0000000000000PVJ', 'STORE_ACCESS', 'Access the central store module',
        NOW(), NOW(), 'system', 'system', 0);

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT r.id, p.id
FROM iam_role r
CROSS JOIN iam_privilege p
WHERE p.name = 'STORE_ACCESS'
  AND r.name IN ('ROOT', 'STORE_PERSON', 'PROCUREMENT');
