-- ============================================================================
-- Phase 32: cashier shifts + end-of-day cash reconciliation
-- (PROCESS.md §11). One open shift per cashier at a time; on close
-- the service compares the declared closing float against the expected
-- (opening + sum of CASH payments captured by the user in window) and
-- stores the variance for audit.
-- ============================================================================

CREATE TABLE cashier_shift (
    id                          BIGSERIAL PRIMARY KEY,
    uid                         VARCHAR(26)   NOT NULL,

    cashier_username            VARCHAR(64)   NOT NULL,
    currency                    VARCHAR(3)    NOT NULL,

    opening_float               NUMERIC(14,2) NOT NULL,
    opened_at                   TIMESTAMP WITH TIME ZONE NOT NULL,

    status                      VARCHAR(16)   NOT NULL,
    closed_at                   TIMESTAMP WITH TIME ZONE,
    closing_declared_amount     NUMERIC(14,2),
    closing_expected_amount     NUMERIC(14,2),
    variance                    NUMERIC(14,2),
    closing_note                VARCHAR(500),

    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by                  VARCHAR(80),
    updated_by                  VARCHAR(80),
    version                     BIGINT,
    CONSTRAINT uk_cashier_shift_uid    UNIQUE (uid),
    CONSTRAINT ck_cashier_shift_float  CHECK (opening_float >= 0)
);

CREATE INDEX idx_cashier_shift_user      ON cashier_shift(cashier_username);
CREATE INDEX idx_cashier_shift_status    ON cashier_shift(status);
CREATE INDEX idx_cashier_shift_opened_at ON cashier_shift(opened_at);
CREATE INDEX idx_cashier_shift_closed_at ON cashier_shift(closed_at);

-- Enforce at most one OPEN shift per cashier via a partial unique index.
CREATE UNIQUE INDEX uk_cashier_shift_one_open_per_user
    ON cashier_shift (cashier_username)
    WHERE status = 'OPEN';
