-- ============================================================================
-- Phase 47: HR payroll (PROCESS.md §17.11 last row).
--
-- Period header + per-employee items. Gross / deductions / net are
-- captured as snapshot figures — no statutory tax tables, no auto-prefill.
-- That's the deliberate V1 scope: the legacy doesn't compute PAYE either;
-- a real HR/finance package owns that surface area.
-- ============================================================================

CREATE TABLE hr_payroll_period (
    id                     BIGSERIAL PRIMARY KEY,
    uid                    VARCHAR(26)  NOT NULL,

    code                   VARCHAR(32)  NOT NULL,
    label                  VARCHAR(80)  NOT NULL,
    start_date             DATE         NOT NULL,
    end_date               DATE         NOT NULL,
    currency               VARCHAR(3)   NOT NULL,
    status                 VARCHAR(16)  NOT NULL,
    note                   VARCHAR(500),

    approved_at            TIMESTAMP WITH TIME ZONE,
    approved_by_username   VARCHAR(64),
    paid_at                TIMESTAMP WITH TIME ZONE,
    cancelled_at           TIMESTAMP WITH TIME ZONE,
    cancel_reason          VARCHAR(500),

    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by             VARCHAR(80),
    updated_by             VARCHAR(80),
    version                BIGINT,
    CONSTRAINT uk_hr_payroll_period_uid  UNIQUE (uid),
    CONSTRAINT uk_hr_payroll_period_code UNIQUE (code),
    CONSTRAINT ck_hr_payroll_period_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_hr_payroll_period_status ON hr_payroll_period(status);

CREATE TABLE hr_payroll_item (
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(26)  NOT NULL,

    period_uid          VARCHAR(26)  NOT NULL,
    employee_uid        VARCHAR(26)  NOT NULL,

    gross_pay           NUMERIC(14,2) NOT NULL,
    total_deductions    NUMERIC(14,2) NOT NULL,
    net_pay             NUMERIC(14,2) NOT NULL,

    payment_method      VARCHAR(32),
    payment_reference   VARCHAR(80),
    note                VARCHAR(500),

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(80),
    updated_by          VARCHAR(80),
    version             BIGINT,
    CONSTRAINT uk_hr_payroll_item_uid              UNIQUE (uid),
    CONSTRAINT uk_hr_payroll_item_period_employee  UNIQUE (period_uid, employee_uid),
    CONSTRAINT ck_hr_payroll_item_gross            CHECK (gross_pay >= 0),
    CONSTRAINT ck_hr_payroll_item_deductions       CHECK (total_deductions >= 0
                                                          AND total_deductions <= gross_pay)
);

CREATE INDEX idx_hr_payroll_item_period   ON hr_payroll_item(period_uid);
CREATE INDEX idx_hr_payroll_item_employee ON hr_payroll_item(employee_uid);
