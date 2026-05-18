-- ============================================================================
-- Phase 26: HR module (PROCESS.md §12, §17.11). Employee aggregate +
-- optional link to iam.User. Payroll deferred — large business surface
-- area, not blocked by this schema.
--
-- ClinicianPerformance is computed live in the service (not persisted).
-- ============================================================================

CREATE SEQUENCE hr_employee_no_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE hr_employee (
    id                       BIGSERIAL PRIMARY KEY,
    uid                      VARCHAR(26)  NOT NULL,
    employee_no              VARCHAR(32)  NOT NULL,

    first_name               VARCHAR(80)  NOT NULL,
    middle_name              VARCHAR(80),
    last_name                VARCHAR(80)  NOT NULL,
    gender                   VARCHAR(16),
    date_of_birth            DATE,
    national_id              VARCHAR(32),
    phone                    VARCHAR(32),
    email                    VARCHAR(120),
    address                  VARCHAR(255),

    username                 VARCHAR(64),
    designation              VARCHAR(120),
    department               VARCHAR(120),

    hire_date                DATE         NOT NULL,
    employment_status        VARCHAR(16)  NOT NULL,
    termination_date         DATE,
    termination_reason       VARCHAR(500),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(80),
    updated_by               VARCHAR(80),
    version                  BIGINT,
    CONSTRAINT uk_hr_employee_uid         UNIQUE (uid),
    CONSTRAINT uk_hr_employee_employee_no UNIQUE (employee_no),
    CONSTRAINT uk_hr_employee_username    UNIQUE (username)
);

CREATE INDEX idx_hr_employee_status      ON hr_employee(employment_status);
CREATE INDEX idx_hr_employee_designation ON hr_employee(designation);
CREATE INDEX idx_hr_employee_department  ON hr_employee(department);
