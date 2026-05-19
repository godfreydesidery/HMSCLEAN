-- ============================================================================
-- Phase 45: lab batch processing (PROCESS.md §5.5, §17.4).
--
-- Groups N same-test LAB_TEST orders so the bench can process them as
-- one run. Purely organisational — individual order statuses are not
-- modified by batch transitions. An order can sit in at most one batch
-- at a time (enforced by the unique constraint on lab_batch_member
-- .order_uid).
-- ============================================================================

CREATE SEQUENCE lab_batch_no_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE lab_batch (
    id                    BIGSERIAL PRIMARY KEY,
    uid                   VARCHAR(26)  NOT NULL,

    batch_no              VARCHAR(32)  NOT NULL,
    lab_test_type_uid     VARCHAR(26)  NOT NULL,
    note                  VARCHAR(500),

    status                VARCHAR(16)  NOT NULL,
    opened_by_username    VARCHAR(64)  NOT NULL,
    opened_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at          TIMESTAMP WITH TIME ZONE,
    completed_at          TIMESTAMP WITH TIME ZONE,
    cancelled_at          TIMESTAMP WITH TIME ZONE,
    cancel_reason         VARCHAR(255),

    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by            VARCHAR(80),
    updated_by            VARCHAR(80),
    version               BIGINT,
    CONSTRAINT uk_lab_batch_uid UNIQUE (uid),
    CONSTRAINT uk_lab_batch_no  UNIQUE (batch_no)
);

CREATE INDEX idx_lab_batch_status ON lab_batch(status);
CREATE INDEX idx_lab_batch_test   ON lab_batch(lab_test_type_uid);

CREATE TABLE lab_batch_member (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,

    batch_uid   VARCHAR(26)  NOT NULL,
    order_uid   VARCHAR(26)  NOT NULL,

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_lab_batch_member_uid   UNIQUE (uid),
    CONSTRAINT uk_lab_batch_member_order UNIQUE (order_uid)
);

CREATE INDEX idx_lab_batch_member_batch ON lab_batch_member(batch_uid);
