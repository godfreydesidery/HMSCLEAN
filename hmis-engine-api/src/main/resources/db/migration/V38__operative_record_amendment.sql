-- ============================================================================
-- Phase 34: append-only amendment trail for locked OperativeRecords.
-- The original op-note stays canonical and locked; corrections live
-- alongside as numbered amendments with a required reason.
-- ============================================================================

CREATE TABLE operative_record_amendment (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)  NOT NULL,

    operative_record_uid     VARCHAR(26)  NOT NULL,
    amendment_no             INTEGER      NOT NULL,
    reason                   VARCHAR(1000) NOT NULL,

    findings                 VARCHAR(4000),
    technique                VARCHAR(4000),
    instruments              VARCHAR(2000),
    complications            VARCHAR(2000),
    specimens                VARCHAR(2000),
    assistants               VARCHAR(500),
    anaesthesia_type         VARCHAR(64),
    scrub_nurse              VARCHAR(120),
    circulating_nurse        VARCHAR(120),

    authored_by_username     VARCHAR(64)  NOT NULL,
    authored_at              TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_operative_record_amendment_uid UNIQUE (uid),
    CONSTRAINT uk_operative_record_amendment_seq UNIQUE (operative_record_uid, amendment_no),
    CONSTRAINT ck_operative_record_amendment_no  CHECK (amendment_no > 0)
);

CREATE INDEX idx_op_record_amend_record   ON operative_record_amendment(operative_record_uid);
CREATE INDEX idx_op_record_amend_authored ON operative_record_amendment(authored_at);
