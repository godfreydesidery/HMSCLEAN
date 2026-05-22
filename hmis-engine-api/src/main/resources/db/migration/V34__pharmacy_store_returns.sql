-- ============================================================================
-- Phase 30: pharmacy → store returns (PROCESS.md §8.5 reverse direction).
-- Single-document flow; the three-doc RO/TO/RN dance is overkill for
-- returns since the pharmacy has full information about what's going back.
-- ============================================================================

CREATE SEQUENCE pharmacy_store_return_no_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE pharmacy_store_return (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)  NOT NULL,
    return_no                VARCHAR(32)  NOT NULL,

    pharmacy_uid             VARCHAR(26)  NOT NULL,
    store_uid                VARCHAR(26)  NOT NULL,

    return_date              DATE         NOT NULL,
    reason                   VARCHAR(500),
    note                     VARCHAR(500),

    status                   VARCHAR(16)  NOT NULL,
    submitted_at             TIMESTAMP WITH TIME ZONE,
    submitted_by_username    VARCHAR(64),
    completed_at             TIMESTAMP WITH TIME ZONE,
    completed_by_username    VARCHAR(64),
    rejected_at              TIMESTAMP WITH TIME ZONE,
    rejected_by_username     VARCHAR(64),
    reject_reason            VARCHAR(255),
    cancelled_at             TIMESTAMP WITH TIME ZONE,

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_p2s_return_uid UNIQUE (uid),
    CONSTRAINT uk_p2s_return_no  UNIQUE (return_no)
);
CREATE INDEX idx_p2s_return_pharmacy    ON pharmacy_store_return(pharmacy_uid);
CREATE INDEX idx_p2s_return_store       ON pharmacy_store_return(store_uid);
CREATE INDEX idx_p2s_return_status      ON pharmacy_store_return(status);
CREATE INDEX idx_p2s_return_return_date ON pharmacy_store_return(return_date);

CREATE TABLE pharmacy_store_return_line (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    return_uid      VARCHAR(26)  NOT NULL,
    medicine_uid    VARCHAR(26)  NOT NULL,
    unit_uid        VARCHAR(26),
    quantity        INTEGER      NOT NULL,
    reason          VARCHAR(500),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_p2s_return_line_uid UNIQUE (uid),
    CONSTRAINT ck_p2s_return_line_qty CHECK (quantity > 0)
);
CREATE INDEX idx_p2s_return_line_return   ON pharmacy_store_return_line(return_uid);
CREATE INDEX idx_p2s_return_line_medicine ON pharmacy_store_return_line(medicine_uid);

CREATE TABLE pharmacy_store_return_batch_pick (
    id                BIGSERIAL PRIMARY KEY,
    uid               VARCHAR(26)  NOT NULL,

    return_line_uid   VARCHAR(26)  NOT NULL,
    source_batch_uid  VARCHAR(26)  NOT NULL,
    batch_no          VARCHAR(64)  NOT NULL,
    expires_at        DATE,
    quantity          INTEGER      NOT NULL,

    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by        VARCHAR(80),
    updated_by        VARCHAR(80),
    version           BIGINT,
    CONSTRAINT uk_p2s_return_pick_uid UNIQUE (uid),
    CONSTRAINT ck_p2s_return_pick_qty CHECK (quantity > 0)
);
CREATE INDEX idx_p2s_return_pick_line  ON pharmacy_store_return_batch_pick(return_line_uid);
CREATE INDEX idx_p2s_return_pick_batch ON pharmacy_store_return_batch_pick(source_batch_uid);
