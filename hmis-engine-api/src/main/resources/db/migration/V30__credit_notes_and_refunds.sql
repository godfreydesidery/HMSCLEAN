-- ============================================================================
-- Phase 25: credit notes + refunds (PROCESS.md §11, §16, §17.10).
--
-- Credit notes are authorised write-downs against an invoice — money
-- changes hands ONLY in the case of a refund. The new invoice column
-- `total_credited` rolls up applied credits; balance = subtotal -
-- total_paid - total_credited.
-- ============================================================================

ALTER TABLE invoice ADD COLUMN total_credited NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE invoice ALTER COLUMN total_credited DROP DEFAULT;

CREATE SEQUENCE credit_note_no_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE refund_no_seq      START WITH 1 INCREMENT BY 1;

CREATE TABLE credit_note (
    id                   BIGSERIAL PRIMARY KEY,
    uid                  VARCHAR(26)   NOT NULL,
    note_no              VARCHAR(32)   NOT NULL,

    invoice_uid          VARCHAR(26)   NOT NULL,
    amount               NUMERIC(14,2) NOT NULL,
    currency             VARCHAR(3)    NOT NULL,
    reason               VARCHAR(24)   NOT NULL,
    description          VARCHAR(500),

    issued_by_username   VARCHAR(64)   NOT NULL,
    issued_at            TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by           VARCHAR(80),
    updated_by           VARCHAR(80),
    version              BIGINT,
    CONSTRAINT uk_credit_note_uid    UNIQUE (uid),
    CONSTRAINT uk_credit_note_no     UNIQUE (note_no),
    CONSTRAINT ck_credit_note_amount CHECK (amount > 0)
);
CREATE INDEX idx_credit_note_invoice ON credit_note(invoice_uid);
CREATE INDEX idx_credit_note_issued  ON credit_note(issued_at);
CREATE INDEX idx_credit_note_reason  ON credit_note(reason);

CREATE TABLE refund (
    id                   BIGSERIAL PRIMARY KEY,
    uid                  VARCHAR(26)   NOT NULL,
    refund_no            VARCHAR(32)   NOT NULL,

    invoice_uid          VARCHAR(26)   NOT NULL,
    amount               NUMERIC(14,2) NOT NULL,
    currency             VARCHAR(3)    NOT NULL,
    method               VARCHAR(24)   NOT NULL,
    reason               VARCHAR(24)   NOT NULL,
    description          VARCHAR(500),
    reference            VARCHAR(80),

    refunded_by_username VARCHAR(64)   NOT NULL,
    refunded_at          TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by           VARCHAR(80),
    updated_by           VARCHAR(80),
    version              BIGINT,
    CONSTRAINT uk_refund_uid    UNIQUE (uid),
    CONSTRAINT uk_refund_no     UNIQUE (refund_no),
    CONSTRAINT ck_refund_amount CHECK (amount > 0)
);
CREATE INDEX idx_refund_invoice  ON refund(invoice_uid);
CREATE INDEX idx_refund_refunded ON refund(refunded_at);
CREATE INDEX idx_refund_reason   ON refund(reason);
