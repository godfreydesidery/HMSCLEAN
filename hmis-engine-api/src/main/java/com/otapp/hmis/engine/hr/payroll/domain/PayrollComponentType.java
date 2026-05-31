package com.otapp.hmis.engine.hr.payroll.domain;

/**
 * Whether a {@link PayrollComponent} adds to or subtracts from pay.
 *
 * <ul>
 *   <li>{@code EARNING}   — an allowance/benefit; increases gross pay.</li>
 *   <li>{@code DEDUCTION} — tax, statutory contribution, loan, etc.; reduces net.</li>
 *   <li>{@code EMPLOYER_CONTRIBUTION} — employer-side cost of employment
 *       (e.g. the employer's social-security match). Tracked for the cost
 *       report but NEVER added to gross nor subtracted from net (legacy
 *       {@code PayrollDetail.employerContributions}).</li>
 * </ul>
 */
public enum PayrollComponentType {
    EARNING,
    DEDUCTION,
    EMPLOYER_CONTRIBUTION
}
