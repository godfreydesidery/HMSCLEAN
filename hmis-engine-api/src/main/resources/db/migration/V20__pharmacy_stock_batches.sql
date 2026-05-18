-- ============================================================================
-- Pharmacy stock now tracks per-batch (lot) quantities. The aggregate
-- (pharmacy_uid, medicine_uid) balance in pharmacy_stock_balance is kept
-- as a fast read-side rollup but is the sum of batches.
--
-- Dispensing follows FEFO (First-Expired-First-Out): the batch with the
-- soonest expiry is depleted first, then the next, until the requested
-- quantity is fulfilled.
-- ============================================================================

CREATE TABLE pharmacy_stock_batch (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    pharmacy_uid    VARCHAR(26)  NOT NULL,
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
    CONSTRAINT uk_stock_batch_uid                          UNIQUE (uid),
    CONSTRAINT uk_stock_batch_pharmacy_medicine_batchno    UNIQUE (pharmacy_uid, medicine_uid, batch_no),
    CONSTRAINT ck_stock_batch_non_negative                 CHECK (quantity >= 0)
);

CREATE INDEX idx_stock_batch_pharmacy_medicine ON pharmacy_stock_batch(pharmacy_uid, medicine_uid);
CREATE INDEX idx_stock_batch_expires           ON pharmacy_stock_batch(expires_at);

-- Movements now reference the batch they affected. Nullable for legacy /
-- aggregate-only movements; required for all new movements.
ALTER TABLE pharmacy_stock_movement
    ADD COLUMN batch_uid VARCHAR(26),
    ADD COLUMN batch_no  VARCHAR(64);

CREATE INDEX idx_stock_movement_batch ON pharmacy_stock_movement(batch_uid);
