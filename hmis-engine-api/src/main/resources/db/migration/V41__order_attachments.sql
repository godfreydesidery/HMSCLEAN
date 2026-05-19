-- ============================================================================
-- Phase 39: file attachments on clinical orders
-- (PROCESS.md §17.4 lab result attachments / §17.5 radiology images).
--
-- Bytes live on disk under the hmis.attachments.dir property; this row
-- carries the metadata + the relative storage_key that points at them.
-- Applies to any ClinicalOrder kind (LAB_TEST / RADIOLOGY / PROCEDURE).
-- ============================================================================

CREATE TABLE order_attachment (
    id                    BIGSERIAL PRIMARY KEY,
    uid                   VARCHAR(26)  NOT NULL,

    order_uid             VARCHAR(26)  NOT NULL,
    order_kind            VARCHAR(16)  NOT NULL,
    filename              VARCHAR(255) NOT NULL,
    content_type          VARCHAR(120) NOT NULL,
    size_bytes            BIGINT       NOT NULL,
    storage_key           VARCHAR(500) NOT NULL,
    description           VARCHAR(500),
    uploaded_by_username  VARCHAR(64)  NOT NULL,
    uploaded_at           TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by            VARCHAR(80),
    updated_by            VARCHAR(80),
    version               BIGINT,
    CONSTRAINT uk_order_attachment_uid  UNIQUE (uid),
    CONSTRAINT ck_order_attachment_size CHECK (size_bytes > 0)
);

CREATE INDEX idx_order_attachment_order    ON order_attachment(order_uid);
CREATE INDEX idx_order_attachment_uploaded ON order_attachment(uploaded_at);
