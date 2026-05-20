package com.otapp.hmis.engine.hr.payroll.domain;

/**
 * Whether a {@link PayrollComponent} adds to or subtracts from pay.
 *
 * <ul>
 *   <li>{@code EARNING}   — an allowance/benefit; increases gross pay.</li>
 *   <li>{@code DEDUCTION} — tax, statutory contribution, loan, etc.; reduces net.</li>
 * </ul>
 */
public enum PayrollComponentType {
    EARNING,
    DEDUCTION
}
