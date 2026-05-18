-- ============================================================================
-- Pharmacy retail / OTC sale orders (PROCESS.md §8.2): a multi-line sale
-- transaction with the proven 8-state per-line lifecycle (PENDING → SOLD).
-- ============================================================================

CREATE SEQUENCE pharmacy_sale_order_no_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE pharmacy_sale_order (
    id                   BIGSERIAL PRIMARY KEY,
    uid                  VARCHAR(26)   NOT NULL,
    sale_no              VARCHAR(32)   NOT NULL,

    pharmacy_uid         VARCHAR(26)   NOT NULL,
    patient_uid          VARCHAR(26),
    customer_name        VARCHAR(160)  NOT NULL,
    customer_phone       VARCHAR(40),

    status               VARCHAR(16)   NOT NULL,
    payment_type         VARCHAR(16)   NOT NULL,
    insurance_plan_uid   VARCHAR(26),

    currency             VARCHAR(3)    NOT NULL,
    subtotal             NUMERIC(14,2) NOT NULL,
    total_paid           NUMERIC(14,2) NOT NULL,

    opened_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at         TIMESTAMP WITH TIME ZONE,
    cancelled_at         TIMESTAMP WITH TIME ZONE,
    cancel_reason        VARCHAR(255),

    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by           VARCHAR(80),
    updated_by           VARCHAR(80),
    version              BIGINT,
    CONSTRAINT uk_pharmacy_sale_order_uid UNIQUE (uid),
    CONSTRAINT uk_pharmacy_sale_order_no  UNIQUE (sale_no)
);

CREATE INDEX idx_pharmacy_sale_order_pharmacy ON pharmacy_sale_order(pharmacy_uid);
CREATE INDEX idx_pharmacy_sale_order_patient  ON pharmacy_sale_order(patient_uid);
CREATE INDEX idx_pharmacy_sale_order_status   ON pharmacy_sale_order(status);
CREATE INDEX idx_pharmacy_sale_order_opened   ON pharmacy_sale_order(opened_at);

CREATE TABLE pharmacy_sale_order_line (
    id                   BIGSERIAL PRIMARY KEY,
    uid                  VARCHAR(26)   NOT NULL,

    sale_uid             VARCHAR(26)   NOT NULL,
    medicine_uid         VARCHAR(26)   NOT NULL,
    quantity             INTEGER       NOT NULL,
    dose                 VARCHAR(80),
    frequency            VARCHAR(80),
    duration_days        INTEGER,
    instructions         VARCHAR(500),

    unit_price           NUMERIC(14,2) NOT NULL,
    line_amount          NUMERIC(14,2) NOT NULL,

    status               VARCHAR(16)   NOT NULL,
    created_at_line      TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at          TIMESTAMP WITH TIME ZONE,
    held_at              TIMESTAMP WITH TIME ZONE,
    verified_at          TIMESTAMP WITH TIME ZONE,
    approved_at          TIMESTAMP WITH TIME ZONE,
    sold_at              TIMESTAMP WITH TIME ZONE,
    rejected_at          TIMESTAMP WITH TIME ZONE,
    reject_reason        VARCHAR(255),
    cancel_reason        VARCHAR(255),

    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by           VARCHAR(80),
    updated_by           VARCHAR(80),
    version              BIGINT,
    CONSTRAINT uk_pharmacy_sale_order_line_uid UNIQUE (uid),
    CONSTRAINT ck_pharmacy_sale_line_qty CHECK (quantity > 0)
);

CREATE INDEX idx_pharmacy_sale_line_sale     ON pharmacy_sale_order_line(sale_uid);
CREATE INDEX idx_pharmacy_sale_line_medicine ON pharmacy_sale_order_line(medicine_uid);
CREATE INDEX idx_pharmacy_sale_line_status   ON pharmacy_sale_order_line(status);
