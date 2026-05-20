package com.otapp.hmis.engine.hr.payroll.domain;

/**
 * The amount a PERCENT/BAND {@link PayrollComponent} is computed against.
 *
 * <ul>
 *   <li>{@code BASIC} — the employee's basic pay (the compute input).</li>
 *   <li>{@code GROSS} — basic + all EARNING components (e.g. PAYE on gross).</li>
 * </ul>
 *
 * <p>Ignored for {@code FIXED}, and EARNING components always compute on basic
 * to avoid a circular gross definition.
 */
public enum PayrollCalcBase {
    BASIC,
    GROSS
}
