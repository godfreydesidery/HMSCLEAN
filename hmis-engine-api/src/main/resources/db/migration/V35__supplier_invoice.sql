-- ============================================================================
-- Phase 31: supplier invoices + three-way procurement match (PROCESS.md
-- §10, §17.9). Match enforced on the APPROVED transition — for each
-- invoice line: invoiced ≤ received ≤ ordered on the matching PO line,
-- cumulative across all approved invoices.
-- ============================================================================

-- PurchaseOrderLine: cumulative invoiced quantity for the match.
ALTER TABLE purchase_order_line ADD COLUMN invoiced_quantity INTEGER NOT NULL DEFAULT 0;
ALTER TABLE purchase_order_line ALTER COLUMN invoiced_quantity DROP DEFAULT;
ALTER TABLE purchase_order_line
    ADD CONSTRAINT ck_purchase_order_line_invoiced
    CHECK (invoiced_quantity >= 0 AND invoiced_quantity <= received_quantity);

-- Supplier invoice header
CREATE TABLE supplier_invoice (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)   NOT NULL,

    supplier_uid             VARCHAR(26)   NOT NULL,
    order_uid                VARCHAR(26)   NOT NULL,
    supplier_invoice_no      VARCHAR(64)   NOT NULL,

    invoice_date             DATE          NOT NULL,
    due_date                 DATE,
    currency                 VARCHAR(3)    NOT NULL,
    total_amount             NUMERIC(14,2) NOT NULL,

    status                   VARCHAR(16)   NOT NULL,

    submitted_at             TIMESTAMP WITH TIME ZONE,
    submitted_by_username    VARCHAR(64),
    approved_at              TIMESTAMP WITH TIME ZONE,
    approved_by_username     VARCHAR(64),
    paid_at                  TIMESTAMP WITH TIME ZONE,
    paid_by_username         VARCHAR(64),
    payment_method           VARCHAR(24),
    payment_reference        VARCHAR(120),
    rejected_at              TIMESTAMP WITH TIME ZONE,
    rejected_by_username     VARCHAR(64),
    reject_reason            VARCHAR(255),
    cancelled_at             TIMESTAMP WITH TIME ZONE,

    notes                    VARCHAR(500),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_supplier_invoice_uid          UNIQUE (uid),
    CONSTRAINT uk_supplier_invoice_supplier_no  UNIQUE (supplier_uid, supplier_invoice_no)
);

CREATE INDEX idx_supplier_invoice_supplier ON supplier_invoice(supplier_uid);
CREATE INDEX idx_supplier_invoice_order    ON supplier_invoice(order_uid);
CREATE INDEX idx_supplier_invoice_status   ON supplier_invoice(status);
CREATE INDEX idx_supplier_invoice_date     ON supplier_invoice(invoice_date);

CREATE TABLE supplier_invoice_line (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)   NOT NULL,

    invoice_uid              VARCHAR(26)   NOT NULL,
    po_line_uid              VARCHAR(26)   NOT NULL,

    invoiced_quantity        INTEGER       NOT NULL,
    unit_cost                NUMERIC(14,2) NOT NULL,
    line_amount              NUMERIC(14,2) NOT NULL,

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_supplier_invoice_line_uid UNIQUE (uid),
    CONSTRAINT ck_supplier_invoice_line_qty CHECK (invoiced_quantity > 0)
);

CREATE INDEX idx_supplier_invoice_line_invoice ON supplier_invoice_line(invoice_uid);
CREATE INDEX idx_supplier_invoice_line_po_line ON supplier_invoice_line(po_line_uid);
