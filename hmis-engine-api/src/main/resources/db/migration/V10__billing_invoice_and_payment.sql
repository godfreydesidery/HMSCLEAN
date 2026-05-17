-- ============================================================================
-- Billing: invoice + invoice_line + payment.
-- An invoice is bound 1:1 to a consultation. Lines are regenerated when
-- the invoice is still DRAFT; once ISSUED, lines are immutable.
-- ============================================================================

CREATE SEQUENCE invoice_no_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE payment_no_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE invoice (
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(26)   NOT NULL,
    invoice_no          VARCHAR(32)   NOT NULL,
    consultation_uid    VARCHAR(26)   NOT NULL,
    patient_uid         VARCHAR(26)   NOT NULL,
    payment_type        VARCHAR(16)   NOT NULL,
    insurance_plan_uid  VARCHAR(26),
    currency            VARCHAR(3)    NOT NULL,
    subtotal            NUMERIC(14,2) NOT NULL,
    total_paid          NUMERIC(14,2) NOT NULL,
    status              VARCHAR(16)   NOT NULL,
    issued_at           TIMESTAMP WITH TIME ZONE,
    paid_at             TIMESTAMP WITH TIME ZONE,
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    cancel_reason       VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(80),
    updated_by          VARCHAR(80),
    version             BIGINT,
    CONSTRAINT uk_invoice_uid          UNIQUE (uid),
    CONSTRAINT uk_invoice_no           UNIQUE (invoice_no),
    CONSTRAINT uk_invoice_consultation UNIQUE (consultation_uid)
);

CREATE INDEX idx_invoice_patient   ON invoice(patient_uid);
CREATE INDEX idx_invoice_status    ON invoice(status);
CREATE INDEX idx_invoice_issued_at ON invoice(issued_at);

CREATE TABLE invoice_line (
    id             BIGSERIAL PRIMARY KEY,
    uid            VARCHAR(26)   NOT NULL,
    invoice_uid    VARCHAR(26)   NOT NULL,
    kind           VARCHAR(16)   NOT NULL,
    service_uid    VARCHAR(26),
    reference_uid  VARCHAR(26),
    description    VARCHAR(255)  NOT NULL,
    quantity       NUMERIC(12,2) NOT NULL,
    unit_price     NUMERIC(14,2) NOT NULL,
    amount         NUMERIC(14,2) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by     VARCHAR(80),
    updated_by     VARCHAR(80),
    version        BIGINT,
    CONSTRAINT uk_invoice_line_uid UNIQUE (uid)
);

CREATE INDEX idx_invoice_line_invoice ON invoice_line(invoice_uid);

CREATE TABLE payment (
    id            BIGSERIAL PRIMARY KEY,
    uid           VARCHAR(26)   NOT NULL,
    payment_no    VARCHAR(32)   NOT NULL,
    invoice_uid   VARCHAR(26)   NOT NULL,
    method        VARCHAR(24)   NOT NULL,
    amount        NUMERIC(14,2) NOT NULL,
    currency      VARCHAR(3)    NOT NULL,
    reference     VARCHAR(80),
    note          VARCHAR(255),
    received_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(80),
    updated_by    VARCHAR(80),
    version       BIGINT,
    CONSTRAINT uk_payment_uid UNIQUE (uid),
    CONSTRAINT uk_payment_no  UNIQUE (payment_no)
);

CREATE INDEX idx_payment_invoice     ON payment(invoice_uid);
CREATE INDEX idx_payment_received_at ON payment(received_at);
