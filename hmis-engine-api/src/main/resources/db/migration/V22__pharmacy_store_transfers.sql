-- ============================================================================
-- Pharmacy ↔ Store transfer chain (PROCESS.md §8.5, Phase 20):
--   1. Pharmacy creates a Request Order (RO) asking the store for stock.
--   2. Store creates a Transfer Order (TO) against the RO and issues it,
--      decrementing store batches FEFO. Per-batch picks are recorded.
--   3. Pharmacy files a Receive Note (RN) confirming what arrived;
--      pharmacy stock is incremented per source batch.
--
-- Forward direction only in this phase; reverse (pharmacy → store returns)
-- and pharmacy ↔ pharmacy transfers slot into later phases.
-- ============================================================================

CREATE SEQUENCE pharmacy_to_store_ro_no_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE store_to_pharmacy_to_no_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE store_to_pharmacy_rn_no_seq START WITH 1 INCREMENT BY 1;

-- ----------------------------------------------------------------------------
-- Request Order (RO)
-- ----------------------------------------------------------------------------

CREATE TABLE pharmacy_to_store_ro (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,
    ro_no           VARCHAR(32)  NOT NULL,

    pharmacy_uid    VARCHAR(26)  NOT NULL,
    store_uid       VARCHAR(26)  NOT NULL,

    order_date      DATE         NOT NULL,
    valid_until     DATE,

    status          VARCHAR(16)  NOT NULL,

    verified_at     TIMESTAMP WITH TIME ZONE,
    approved_at     TIMESTAMP WITH TIME ZONE,
    submitted_at    TIMESTAMP WITH TIME ZONE,
    in_process_at   TIMESTAMP WITH TIME ZONE,
    issued_at       TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    rejected_at     TIMESTAMP WITH TIME ZONE,
    returned_at     TIMESTAMP WITH TIME ZONE,
    reject_reason   VARCHAR(255),
    note            VARCHAR(500),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_p2s_ro_uid UNIQUE (uid),
    CONSTRAINT uk_p2s_ro_no  UNIQUE (ro_no)
);

CREATE INDEX idx_p2s_ro_pharmacy   ON pharmacy_to_store_ro(pharmacy_uid);
CREATE INDEX idx_p2s_ro_store      ON pharmacy_to_store_ro(store_uid);
CREATE INDEX idx_p2s_ro_status     ON pharmacy_to_store_ro(status);
CREATE INDEX idx_p2s_ro_order_date ON pharmacy_to_store_ro(order_date);

CREATE TABLE pharmacy_to_store_ro_line (
    id                   BIGSERIAL PRIMARY KEY,
    uid                  VARCHAR(26)  NOT NULL,

    ro_uid               VARCHAR(26)  NOT NULL,
    medicine_uid         VARCHAR(26)  NOT NULL,

    requested_quantity   INTEGER      NOT NULL,
    fulfilled_quantity   INTEGER      NOT NULL,
    note                 VARCHAR(500),

    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by           VARCHAR(80),
    updated_by           VARCHAR(80),
    version              BIGINT,
    CONSTRAINT uk_p2s_ro_line_uid UNIQUE (uid),
    CONSTRAINT ck_p2s_ro_line_qty CHECK (requested_quantity > 0
                                         AND fulfilled_quantity >= 0
                                         AND fulfilled_quantity <= requested_quantity)
);

CREATE INDEX idx_p2s_ro_line_ro       ON pharmacy_to_store_ro_line(ro_uid);
CREATE INDEX idx_p2s_ro_line_medicine ON pharmacy_to_store_ro_line(medicine_uid);

-- ----------------------------------------------------------------------------
-- Transfer Order (TO)
-- ----------------------------------------------------------------------------

CREATE TABLE store_to_pharmacy_to (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,
    to_no           VARCHAR(32)  NOT NULL,

    ro_uid          VARCHAR(26)  NOT NULL,
    pharmacy_uid    VARCHAR(26)  NOT NULL,
    store_uid       VARCHAR(26)  NOT NULL,

    order_date      DATE         NOT NULL,
    status          VARCHAR(16)  NOT NULL,

    verified_at     TIMESTAMP WITH TIME ZONE,
    approved_at     TIMESTAMP WITH TIME ZONE,
    issued_at       TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    rejected_at     TIMESTAMP WITH TIME ZONE,
    rejected_reason VARCHAR(255),
    note            VARCHAR(500),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_s2p_to_uid UNIQUE (uid),
    CONSTRAINT uk_s2p_to_no  UNIQUE (to_no)
);

CREATE INDEX idx_s2p_to_pharmacy   ON store_to_pharmacy_to(pharmacy_uid);
CREATE INDEX idx_s2p_to_store      ON store_to_pharmacy_to(store_uid);
CREATE INDEX idx_s2p_to_ro         ON store_to_pharmacy_to(ro_uid);
CREATE INDEX idx_s2p_to_status     ON store_to_pharmacy_to(status);
CREATE INDEX idx_s2p_to_order_date ON store_to_pharmacy_to(order_date);

CREATE TABLE store_to_pharmacy_to_line (
    id                   BIGSERIAL PRIMARY KEY,
    uid                  VARCHAR(26)  NOT NULL,

    to_uid               VARCHAR(26)  NOT NULL,
    ro_line_uid          VARCHAR(26)  NOT NULL,
    medicine_uid         VARCHAR(26)  NOT NULL,

    requested_quantity   INTEGER      NOT NULL,
    issued_quantity      INTEGER      NOT NULL,
    received_quantity    INTEGER      NOT NULL,

    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by           VARCHAR(80),
    updated_by           VARCHAR(80),
    version              BIGINT,
    CONSTRAINT uk_s2p_to_line_uid UNIQUE (uid),
    CONSTRAINT ck_s2p_to_line_qty CHECK (requested_quantity > 0
                                         AND issued_quantity >= 0
                                         AND issued_quantity   <= requested_quantity
                                         AND received_quantity >= 0
                                         AND received_quantity <= issued_quantity)
);

CREATE INDEX idx_s2p_to_line_to       ON store_to_pharmacy_to_line(to_uid);
CREATE INDEX idx_s2p_to_line_ro_line  ON store_to_pharmacy_to_line(ro_line_uid);
CREATE INDEX idx_s2p_to_line_medicine ON store_to_pharmacy_to_line(medicine_uid);

CREATE TABLE store_to_pharmacy_to_batch_pick (
    id                BIGSERIAL PRIMARY KEY,
    uid               VARCHAR(26)  NOT NULL,

    to_line_uid       VARCHAR(26)  NOT NULL,
    source_batch_uid  VARCHAR(26)  NOT NULL,
    batch_no          VARCHAR(64)  NOT NULL,
    expires_at        DATE,
    quantity          INTEGER      NOT NULL,
    rn_line_uid       VARCHAR(26),

    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by        VARCHAR(80),
    updated_by        VARCHAR(80),
    version           BIGINT,
    CONSTRAINT uk_s2p_to_pick_uid UNIQUE (uid),
    CONSTRAINT ck_s2p_to_pick_qty CHECK (quantity > 0)
);

CREATE INDEX idx_s2p_to_pick_line    ON store_to_pharmacy_to_batch_pick(to_line_uid);
CREATE INDEX idx_s2p_to_pick_batch   ON store_to_pharmacy_to_batch_pick(source_batch_uid);
CREATE INDEX idx_s2p_to_pick_rn_line ON store_to_pharmacy_to_batch_pick(rn_line_uid);

-- ----------------------------------------------------------------------------
-- Receive Note (RN)
-- ----------------------------------------------------------------------------

CREATE TABLE store_to_pharmacy_rn (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,
    rn_no           VARCHAR(32)  NOT NULL,

    to_uid          VARCHAR(26)  NOT NULL,
    pharmacy_uid    VARCHAR(26)  NOT NULL,
    store_uid       VARCHAR(26)  NOT NULL,

    receiving_date  DATE         NOT NULL,
    status          VARCHAR(16)  NOT NULL,

    completed_at    TIMESTAMP WITH TIME ZONE,
    cancelled_at    TIMESTAMP WITH TIME ZONE,
    note            VARCHAR(500),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_s2p_rn_uid UNIQUE (uid),
    CONSTRAINT uk_s2p_rn_no  UNIQUE (rn_no)
);

CREATE INDEX idx_s2p_rn_pharmacy       ON store_to_pharmacy_rn(pharmacy_uid);
CREATE INDEX idx_s2p_rn_store          ON store_to_pharmacy_rn(store_uid);
CREATE INDEX idx_s2p_rn_to             ON store_to_pharmacy_rn(to_uid);
CREATE INDEX idx_s2p_rn_status         ON store_to_pharmacy_rn(status);
CREATE INDEX idx_s2p_rn_receiving_date ON store_to_pharmacy_rn(receiving_date);

CREATE TABLE store_to_pharmacy_rn_line (
    id                BIGSERIAL PRIMARY KEY,
    uid               VARCHAR(26)  NOT NULL,

    rn_uid            VARCHAR(26)  NOT NULL,
    to_line_uid       VARCHAR(26)  NOT NULL,
    medicine_uid      VARCHAR(26)  NOT NULL,

    issued_quantity   INTEGER      NOT NULL,
    received_quantity INTEGER      NOT NULL,

    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by        VARCHAR(80),
    updated_by        VARCHAR(80),
    version           BIGINT,
    CONSTRAINT uk_s2p_rn_line_uid UNIQUE (uid),
    CONSTRAINT ck_s2p_rn_line_qty CHECK (issued_quantity > 0
                                         AND received_quantity >= 0
                                         AND received_quantity <= issued_quantity)
);

CREATE INDEX idx_s2p_rn_line_rn       ON store_to_pharmacy_rn_line(rn_uid);
CREATE INDEX idx_s2p_rn_line_to_line  ON store_to_pharmacy_rn_line(to_line_uid);
CREATE INDEX idx_s2p_rn_line_medicine ON store_to_pharmacy_rn_line(medicine_uid);
