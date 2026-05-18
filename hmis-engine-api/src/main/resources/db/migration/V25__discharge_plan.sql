-- ============================================================================
-- Structured discharge plan (PROCESS.md §3.3, §17.14 item 8).
--
-- One plan per admission (1:1, unique constraint). Three kinds — DISCHARGE,
-- DECEASED, REFERRAL — share the same clinical narrative fields and differ
-- only by kind-specific extras. Approval of the plan (PENDING → APPROVED)
-- is what drives the underlying admission to its terminal state.
-- ============================================================================

CREATE TABLE discharge_plan (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)  NOT NULL,

    admission_uid            VARCHAR(26)  NOT NULL,
    kind                     VARCHAR(16)  NOT NULL,
    status                   VARCHAR(16)  NOT NULL,

    history                  VARCHAR(4000),
    investigation            VARCHAR(4000),
    management               VARCHAR(4000),
    operation_note           VARCHAR(4000),
    icu_note                 VARCHAR(4000),
    recommendations          VARCHAR(4000),

    referral_facility        VARCHAR(200),
    referral_reason          VARCHAR(1000),

    time_of_death            TIMESTAMP WITH TIME ZONE,
    cause_of_death           VARCHAR(500),

    authored_by_username     VARCHAR(64)  NOT NULL,
    authored_at              TIMESTAMP WITH TIME ZONE NOT NULL,

    approved_by_username     VARCHAR(64),
    approved_at              TIMESTAMP WITH TIME ZONE,

    cancelled_by_username    VARCHAR(64),
    cancelled_at             TIMESTAMP WITH TIME ZONE,
    cancel_reason            VARCHAR(255),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_discharge_plan_uid       UNIQUE (uid),
    CONSTRAINT uk_discharge_plan_admission UNIQUE (admission_uid)
);

CREATE INDEX idx_discharge_plan_status   ON discharge_plan(status);
CREATE INDEX idx_discharge_plan_kind     ON discharge_plan(kind);
CREATE INDEX idx_discharge_plan_authored ON discharge_plan(authored_at);
