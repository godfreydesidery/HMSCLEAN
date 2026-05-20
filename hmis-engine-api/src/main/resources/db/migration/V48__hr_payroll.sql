-- ============================================================================
-- Phase 47: HR payroll (PROCESS.md §17.11 last row).
--
-- Period header + per-employee items. Gross / deductions / net are
-- captured as snapshot figures. Those figures can be auto-prefilled from
-- configurable components (no hard-coded statutory rates) — see the
-- "Configurable payroll components" block appended below.
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

-- ----------------------------------------------------------------------------
-- Configurable payroll components (FRONTEND_GAPS.md section E/F).
--
-- Replaces the original "no auto-prefill" cut with a data-driven model: HR
-- defines earning/deduction components (FIXED / PERCENT / BAND) and the
-- compute endpoint auto-prefills a payroll item's gross + deductions from a
-- basic salary. No statutory rates are hard-coded — bands/rates are data.
-- ----------------------------------------------------------------------------

CREATE TABLE hr_payroll_component (
    id            BIGSERIAL PRIMARY KEY,
    uid           VARCHAR(26)  NOT NULL,

    code          VARCHAR(32)  NOT NULL,
    name          VARCHAR(120) NOT NULL,
    type          VARCHAR(16)  NOT NULL,   -- EARNING | DEDUCTION
    method        VARCHAR(16)  NOT NULL,   -- FIXED | PERCENT | BAND
    base          VARCHAR(16)  NOT NULL,   -- BASIC | GROSS

    fixed_amount  NUMERIC(14,2),           -- FIXED only
    percent_rate  NUMERIC(9,6),            -- PERCENT only (fraction, 0.1 = 10%)

    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order    INTEGER      NOT NULL DEFAULT 0,

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(80),
    updated_by    VARCHAR(80),
    version       BIGINT,
    CONSTRAINT uk_hr_payroll_component_uid  UNIQUE (uid),
    CONSTRAINT uk_hr_payroll_component_code UNIQUE (code)
);

CREATE INDEX idx_hr_payroll_component_active ON hr_payroll_component(active);
CREATE INDEX idx_hr_payroll_component_type   ON hr_payroll_component(type);

CREATE TABLE hr_payroll_component_band (
    id            BIGSERIAL PRIMARY KEY,
    uid           VARCHAR(26)  NOT NULL,

    component_uid VARCHAR(26)  NOT NULL,
    sort_order    INTEGER      NOT NULL,
    from_amount   NUMERIC(14,2) NOT NULL,
    to_amount     NUMERIC(14,2),           -- NULL = open-ended top band
    rate          NUMERIC(9,6)  NOT NULL,

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(80),
    updated_by    VARCHAR(80),
    version       BIGINT,
    CONSTRAINT uk_hr_payroll_component_band_uid UNIQUE (uid),
    CONSTRAINT ck_hr_payroll_band_range CHECK (to_amount IS NULL OR to_amount > from_amount),
    CONSTRAINT ck_hr_payroll_band_amounts CHECK (from_amount >= 0 AND rate >= 0)
);

CREATE INDEX idx_hr_payroll_band_component ON hr_payroll_component_band(component_uid);
