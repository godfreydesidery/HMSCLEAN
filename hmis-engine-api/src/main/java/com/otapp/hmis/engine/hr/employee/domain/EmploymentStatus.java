package com.otapp.hmis.engine.hr.employee.domain;

/**
 * Current employment state. Tracked separately from {@code User.enabled}
 * so a system account can be disabled (e.g. on leave) without firing
 * the employee, and an employee can be terminated without their User
 * row being deleted (kept for audit / payroll history).
 */
public enum EmploymentStatus {
    ACTIVE,
    ON_LEAVE,
    SUSPENDED,
    TERMINATED
}
