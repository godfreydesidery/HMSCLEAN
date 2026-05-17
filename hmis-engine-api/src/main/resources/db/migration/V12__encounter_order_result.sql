-- ============================================================================
-- Encounter module: result document for each clinical order
-- (lab test, radiology, procedure).
-- ============================================================================

CREATE TABLE order_result (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    order_uid       VARCHAR(26)  NOT NULL,
    order_kind      VARCHAR(16)  NOT NULL,

    status          VARCHAR(16)  NOT NULL,
    narrative       VARCHAR(8000),
    impression      VARCHAR(1000),

    finalized_at    TIMESTAMP WITH TIME ZONE,
    finalized_by    VARCHAR(80),
    amended_at      TIMESTAMP WITH TIME ZONE,
    amended_by      VARCHAR(80),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_order_result_uid   UNIQUE (uid),
    CONSTRAINT uk_order_result_order UNIQUE (order_uid)
);

CREATE INDEX idx_order_result_status ON order_result(status);
