-- ============================================================================
-- Procurement module: suppliers, purchase orders + lines, goods receipts.
-- ============================================================================

CREATE SEQUENCE purchase_order_no_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE goods_receipt_no_seq  START WITH 1 INCREMENT BY 1;

CREATE TABLE supplier (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)   NOT NULL,
    code            VARCHAR(32)   NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    contact_name    VARCHAR(120),
    phone           VARCHAR(32),
    email           VARCHAR(120),
    address         VARCHAR(255),
    tax_id          VARCHAR(64),
    notes           VARCHAR(500),
    active          BOOLEAN       NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_supplier_uid  UNIQUE (uid),
    CONSTRAINT uk_supplier_code UNIQUE (code)
);

CREATE TABLE purchase_order (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)  NOT NULL,
    order_no                 VARCHAR(32)  NOT NULL,

    supplier_uid             VARCHAR(26)  NOT NULL,
    pharmacy_uid             VARCHAR(26)  NOT NULL,

    status                   VARCHAR(24)  NOT NULL,
    expected_delivery_date   DATE,
    notes                    VARCHAR(500),

    ordered_at               TIMESTAMP WITH TIME ZONE,
    received_at              TIMESTAMP WITH TIME ZONE,
    cancelled_at             TIMESTAMP WITH TIME ZONE,
    cancel_reason            VARCHAR(255),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_purchase_order_uid UNIQUE (uid),
    CONSTRAINT uk_purchase_order_no  UNIQUE (order_no)
);

CREATE INDEX idx_purchase_order_supplier ON purchase_order(supplier_uid);
CREATE INDEX idx_purchase_order_pharmacy ON purchase_order(pharmacy_uid);
CREATE INDEX idx_purchase_order_status   ON purchase_order(status);

CREATE TABLE purchase_order_line (
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(26)   NOT NULL,

    order_uid           VARCHAR(26)   NOT NULL,
    medicine_uid        VARCHAR(26)   NOT NULL,
    ordered_quantity    INTEGER       NOT NULL,
    received_quantity   INTEGER       NOT NULL,
    unit_cost           NUMERIC(14,2) NOT NULL,
    currency            VARCHAR(3)    NOT NULL,

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(80),
    updated_by          VARCHAR(80),
    version             BIGINT,
    CONSTRAINT uk_purchase_order_line_uid UNIQUE (uid),
    CONSTRAINT ck_po_line_ordered_qty CHECK (ordered_quantity > 0),
    CONSTRAINT ck_po_line_received_qty CHECK (received_quantity >= 0 AND received_quantity <= ordered_quantity)
);

CREATE INDEX idx_purchase_order_line_order    ON purchase_order_line(order_uid);
CREATE INDEX idx_purchase_order_line_medicine ON purchase_order_line(medicine_uid);

CREATE TABLE goods_receipt (
    id                      BIGSERIAL PRIMARY KEY,
    uid                     VARCHAR(26)  NOT NULL,
    receipt_no              VARCHAR(32)  NOT NULL,

    order_uid               VARCHAR(26)  NOT NULL,
    pharmacy_uid            VARCHAR(26)  NOT NULL,

    received_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    received_by_username    VARCHAR(64),
    delivery_note           VARCHAR(120),
    notes                   VARCHAR(500),

    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by              VARCHAR(80),
    updated_by              VARCHAR(80),
    version                 BIGINT,
    CONSTRAINT uk_goods_receipt_uid UNIQUE (uid),
    CONSTRAINT uk_goods_receipt_no  UNIQUE (receipt_no)
);

CREATE INDEX idx_goods_receipt_order       ON goods_receipt(order_uid);
CREATE INDEX idx_goods_receipt_pharmacy    ON goods_receipt(pharmacy_uid);
CREATE INDEX idx_goods_receipt_received_at ON goods_receipt(received_at);

CREATE TABLE goods_receipt_line (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    receipt_uid     VARCHAR(26)  NOT NULL,
    po_line_uid     VARCHAR(26)  NOT NULL,
    medicine_uid    VARCHAR(26)  NOT NULL,
    quantity        INTEGER      NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_goods_receipt_line_uid UNIQUE (uid),
    CONSTRAINT ck_goods_receipt_line_qty CHECK (quantity > 0)
);

CREATE INDEX idx_goods_receipt_line_receipt ON goods_receipt_line(receipt_uid);
CREATE INDEX idx_goods_receipt_line_po_line ON goods_receipt_line(po_line_uid);
