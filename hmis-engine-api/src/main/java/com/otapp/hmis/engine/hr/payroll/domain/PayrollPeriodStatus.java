package com.otapp.hmis.engine.hr.payroll.domain;

/**
 * Lifecycle of a payroll period.
 *
 * <pre>
 *   DRAFT ──► APPROVED ──► PAID
 *        \                 (terminal)
 *         └─► CANCELLED
 * </pre>
 *
 * <p>{@code DRAFT} is the only state in which items can be added /
 * updated / removed. {@code APPROVED} locks the items so HR can prepare
 * the disbursement run. {@code PAID} is terminal.
 */
public enum PayrollPeriodStatus {
    DRAFT,
    APPROVED,
    PAID,
    CANCELLED
}
