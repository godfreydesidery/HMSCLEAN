-- ============================================================================
-- Payroll item itemisation / the legacy PayrollDetail breakdown
-- (PROCESS_MISMATCHES.md M24): per-employee earning / deduction lines that make
-- up the gross + deductions on a payroll item. Snapshot rows (populated from the
-- PayrollComponent compute) so later rate changes don't rewrite history.
-- ============================================================================

CREATE TABLE hr_payroll_item_line (
    id          BIGSERIAL    PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,
    item_uid    VARCHAR(26)  NOT NULL,
    code        VARCHAR(32),
    name        VARCHAR(120) NOT NULL,
    type        VARCHAR(16)  NOT NULL,
    amount      NUMERIC(14,2) NOT NULL,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_hr_payroll_item_line_uid UNIQUE (uid)
);

CREATE INDEX idx_hr_payroll_item_line_item ON hr_payroll_item_line(item_uid);
