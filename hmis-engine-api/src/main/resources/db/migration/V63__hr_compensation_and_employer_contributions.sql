-- ============================================================================
-- Phase 63: HR compensation/banking/statutory fields + employer contributions.
--
-- Additive only. Restores the legacy (com.orbix.api) Employee compensation
-- data (basicSalary, bank trio, tinNo, social-security pair, payable flag) and
-- the PayrollDetail.employerContributions figure (employer-side cost-of-
-- employment that is tracked but NEVER subtracted from net pay).
--
-- The period lifecycle, component engine, and net = gross - deductions formula
-- are untouched (already at parity — see V48/V54/V55).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Employee compensation / banking / statutory fields.
--   * legacy used `double basicSalary` — corrected to NUMERIC(14,2).
--   * legacy made tinNo @NotBlank + unique — kept nullable for additive safety
--     (pre-existing rows have none); uniqueness enforced via a PARTIAL index so
--     it only applies when a TIN is actually present.
--   * legacy `active` maps to the existing employment_status = ACTIVE; only the
--     `payable` per-employee enrolment gate is genuinely new (default TRUE).
-- ----------------------------------------------------------------------------
ALTER TABLE hr_employee
    ADD COLUMN basic_salary          NUMERIC(14,2),
    ADD COLUMN tin_no                VARCHAR(32),
    ADD COLUMN bank_name             VARCHAR(120),
    ADD COLUMN bank_account_no       VARCHAR(40),
    ADD COLUMN bank_account_name     VARCHAR(120),
    ADD COLUMN social_security_no    VARCHAR(40),
    ADD COLUMN social_security_name  VARCHAR(120),
    ADD COLUMN payable               BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE hr_employee
    ADD CONSTRAINT ck_hr_employee_basic_salary CHECK (basic_salary IS NULL OR basic_salary >= 0);

CREATE INDEX idx_hr_employee_payable ON hr_employee(payable);

-- Legacy-strict uniqueness on TIN, but only when present (additive-safe).
CREATE UNIQUE INDEX uk_hr_employee_tin_no ON hr_employee(tin_no) WHERE tin_no IS NOT NULL;

-- ----------------------------------------------------------------------------
-- PayrollItem employer contributions (the legacy PayrollDetail.employer-
-- Contributions). Employer-side cost — tracked, NOT part of gross or net.
-- ----------------------------------------------------------------------------
ALTER TABLE hr_payroll_item
    ADD COLUMN employer_contributions NUMERIC(14,2) NOT NULL DEFAULT 0;

ALTER TABLE hr_payroll_item
    ADD CONSTRAINT ck_hr_payroll_item_employer CHECK (employer_contributions >= 0);

-- PayrollComponentType is stored as text; the new EMPLOYER_CONTRIBUTION value
-- needs no DDL. PayrollItemLine.type already persists any component type.
