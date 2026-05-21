package com.otapp.hmis.engine.hr.payroll.domain;

/**
 * Lifecycle of a payroll period (PROCESS_MISMATCHES.md M19 — legacy
 * DRAFT → VERIFIED → APPROVED → PAID, two-step sign-off restored).
 *
 * <pre>
 *   DRAFT ──► VERIFIED ──► APPROVED ──► PAID
 *        \         \                    (terminal)
 *         └─────────┴─► CANCELLED
 * </pre>
 *
 * <p>{@code DRAFT} is the only state in which items can be added / updated /
 * removed. {@code VERIFIED} is the manager checkpoint (items locked);
 * {@code APPROVED} is the director sign-off that readies the disbursement run.
 * {@code PAID} is terminal.
 */
public enum PayrollPeriodStatus {
    DRAFT,
    VERIFIED,
    APPROVED,
    PAID,
    CANCELLED
}
