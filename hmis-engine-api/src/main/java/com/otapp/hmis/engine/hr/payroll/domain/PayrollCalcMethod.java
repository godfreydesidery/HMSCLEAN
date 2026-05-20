package com.otapp.hmis.engine.hr.payroll.domain;

/**
 * How a {@link PayrollComponent}'s amount is derived.
 *
 * <ul>
 *   <li>{@code FIXED}   — a flat money amount, base-independent.</li>
 *   <li>{@code PERCENT} — a single rate applied to the chosen base.</li>
 *   <li>{@code BAND}    — a progressive table of {@link PayrollComponentBand}
 *       rows (e.g. PAYE): each band's rate applies only to the slice of the
 *       base that falls within it.</li>
 * </ul>
 */
public enum PayrollCalcMethod {
    FIXED,
    PERCENT,
    BAND
}
