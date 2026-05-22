-- ============================================================================
-- Pharmacy ↔ Pharmacy transfer chain (PROCESS.md §8.4, Phase 20b):
--   1. Requesting pharmacy creates a Request Order (RO) for the delivering
--      pharmacy.
--   2. Delivering pharmacy creates a Transfer Order (TO) against the RO
--      and issues it, decrementing its batches FEFO via TRANSFER_OUT.
--   3. Requesting pharmacy files a Receive Note (RN) confirming what
--      arrived; its stock is incremented per source batch via TRANSFER_IN.
--
-- Same shape as the P↔S chain but both endpoints are pharmacies and the
-- stock movement kinds differ (TRANSFER_OUT / TRANSFER_IN, not ISSUE / RECEIPT).
-- ============================================================================

CREATE SEQUENCE pharmacy_to_pharmacy_ro_no_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE pharmacy_to_pharmacy_to_no_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE pharmacy_to_pharmacy_rn_no_seq START WITH 1 INCREMENT BY 1;

-- ----------------------------------------------------------------------------
-- Request Order (RO)
-- ----------------------------------------------------------------------------

CREATE TABLE pharmacy_to_pharmacy_ro (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)  NOT NULL,
    ro_no                    VARCHAR(32)  NOT NULL,

    requesting_pharmacy_uid  VARCHAR(26)  NOT NULL,
    delivering_pharmacy_uid  VARCHAR(26)  NOT NULL,

    order_date               DATE         NOT NULL,
    valid_until              DATE,

    status                   VARCHAR(16)  NOT NULL,

    verified_at              TIMESTAMP WITH TIME ZONE,
    approved_at              TIMESTAMP WITH TIME ZONE,
    submitted_at             TIMESTAMP WITH TIME ZONE,
    in_process_at            TIMESTAMP WITH TIME ZONE,
    issued_at                TIMESTAMP WITH TIME ZONE,
    completed_at             TIMESTAMP WITH TIME ZONE,
    rejected_at              TIMESTAMP WITH TIME ZONE,
    returned_at              TIMESTAMP WITH TIME ZONE,
    reject_reason            VARCHAR(255),
    note                     VARCHAR(500),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_p2p_ro_uid UNIQUE (uid),
    CONSTRAINT uk_p2p_ro_no  UNIQUE (ro_no),
    CONSTRAINT ck_p2p_ro_different_pharmacies CHECK (
        requesting_pharmacy_uid <> delivering_pharmacy_uid)
);

CREATE INDEX idx_p2p_ro_requester  ON pharmacy_to_pharmacy_ro(requesting_pharmacy_uid);
CREATE INDEX idx_p2p_ro_deliverer  ON pharmacy_to_pharmacy_ro(delivering_pharmacy_uid);
CREATE INDEX idx_p2p_ro_status     ON pharmacy_to_pharmacy_ro(status);
CREATE INDEX idx_p2p_ro_order_date ON pharmacy_to_pharmacy_ro(order_date);

CREATE TABLE pharmacy_to_pharmacy_ro_line (
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
    CONSTRAINT uk_p2p_ro_line_uid UNIQUE (uid),
    CONSTRAINT ck_p2p_ro_line_qty CHECK (requested_quantity > 0
                                         AND fulfilled_quantity >= 0
                                         AND fulfilled_quantity <= requested_quantity)
);

CREATE INDEX idx_p2p_ro_line_ro       ON pharmacy_to_pharmacy_ro_line(ro_uid);
CREATE INDEX idx_p2p_ro_line_medicine ON pharmacy_to_pharmacy_ro_line(medicine_uid);

-- ----------------------------------------------------------------------------
-- Transfer Order (TO)
-- ----------------------------------------------------------------------------

CREATE TABLE pharmacy_to_pharmacy_to (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)  NOT NULL,
    to_no                    VARCHAR(32)  NOT NULL,

    ro_uid                   VARCHAR(26)  NOT NULL,
    requesting_pharmacy_uid  VARCHAR(26)  NOT NULL,
    delivering_pharmacy_uid  VARCHAR(26)  NOT NULL,

    order_date               DATE         NOT NULL,
    status                   VARCHAR(16)  NOT NULL,

    verified_at              TIMESTAMP WITH TIME ZONE,
    approved_at              TIMESTAMP WITH TIME ZONE,
    issued_at                TIMESTAMP WITH TIME ZONE,
    completed_at             TIMESTAMP WITH TIME ZONE,
    rejected_at              TIMESTAMP WITH TIME ZONE,
    rejected_reason          VARCHAR(255),
    note                     VARCHAR(500),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_p2p_to_uid UNIQUE (uid),
    CONSTRAINT uk_p2p_to_no  UNIQUE (to_no)
);

CREATE INDEX idx_p2p_to_requester  ON pharmacy_to_pharmacy_to(requesting_pharmacy_uid);
CREATE INDEX idx_p2p_to_deliverer  ON pharmacy_to_pharmacy_to(delivering_pharmacy_uid);
CREATE INDEX idx_p2p_to_ro         ON pharmacy_to_pharmacy_to(ro_uid);
CREATE INDEX idx_p2p_to_status     ON pharmacy_to_pharmacy_to(status);
CREATE INDEX idx_p2p_to_order_date ON pharmacy_to_pharmacy_to(order_date);

CREATE TABLE pharmacy_to_pharmacy_to_line (
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
    CONSTRAINT uk_p2p_to_line_uid UNIQUE (uid),
    CONSTRAINT ck_p2p_to_line_qty CHECK (requested_quantity > 0
                                         AND issued_quantity >= 0
                                         AND issued_quantity   <= requested_quantity
                                         AND received_quantity >= 0
                                         AND received_quantity <= issued_quantity)
);

CREATE INDEX idx_p2p_to_line_to       ON pharmacy_to_pharmacy_to_line(to_uid);
CREATE INDEX idx_p2p_to_line_ro_line  ON pharmacy_to_pharmacy_to_line(ro_line_uid);
CREATE INDEX idx_p2p_to_line_medicine ON pharmacy_to_pharmacy_to_line(medicine_uid);

CREATE TABLE pharmacy_to_pharmacy_to_batch_pick (
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
    CONSTRAINT uk_p2p_to_pick_uid UNIQUE (uid),
    CONSTRAINT ck_p2p_to_pick_qty CHECK (quantity > 0)
);

CREATE INDEX idx_p2p_to_pick_line    ON pharmacy_to_pharmacy_to_batch_pick(to_line_uid);
CREATE INDEX idx_p2p_to_pick_batch   ON pharmacy_to_pharmacy_to_batch_pick(source_batch_uid);
CREATE INDEX idx_p2p_to_pick_rn_line ON pharmacy_to_pharmacy_to_batch_pick(rn_line_uid);

-- ----------------------------------------------------------------------------
-- Receive Note (RN)
-- ----------------------------------------------------------------------------

CREATE TABLE pharmacy_to_pharmacy_rn (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)  NOT NULL,
    rn_no                    VARCHAR(32)  NOT NULL,

    to_uid                   VARCHAR(26)  NOT NULL,
    requesting_pharmacy_uid  VARCHAR(26)  NOT NULL,
    delivering_pharmacy_uid  VARCHAR(26)  NOT NULL,

    receiving_date           DATE         NOT NULL,
    status                   VARCHAR(16)  NOT NULL,

    completed_at             TIMESTAMP WITH TIME ZONE,
    cancelled_at             TIMESTAMP WITH TIME ZONE,
    note                     VARCHAR(500),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_p2p_rn_uid UNIQUE (uid),
    CONSTRAINT uk_p2p_rn_no  UNIQUE (rn_no)
);

CREATE INDEX idx_p2p_rn_requester      ON pharmacy_to_pharmacy_rn(requesting_pharmacy_uid);
CREATE INDEX idx_p2p_rn_deliverer      ON pharmacy_to_pharmacy_rn(delivering_pharmacy_uid);
CREATE INDEX idx_p2p_rn_to             ON pharmacy_to_pharmacy_rn(to_uid);
CREATE INDEX idx_p2p_rn_status         ON pharmacy_to_pharmacy_rn(status);
CREATE INDEX idx_p2p_rn_receiving_date ON pharmacy_to_pharmacy_rn(receiving_date);

CREATE TABLE pharmacy_to_pharmacy_rn_line (
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
    CONSTRAINT uk_p2p_rn_line_uid UNIQUE (uid),
    CONSTRAINT ck_p2p_rn_line_qty CHECK (issued_quantity > 0
                                         AND received_quantity >= 0
                                         AND received_quantity <= issued_quantity)
);

CREATE INDEX idx_p2p_rn_line_rn       ON pharmacy_to_pharmacy_rn_line(rn_uid);
CREATE INDEX idx_p2p_rn_line_to_line  ON pharmacy_to_pharmacy_rn_line(to_line_uid);
CREATE INDEX idx_p2p_rn_line_medicine ON pharmacy_to_pharmacy_rn_line(medicine_uid);
