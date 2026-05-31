package com.otapp.hmis.engine.masterdata.labtest.domain;

/**
 * What kind of value an analyte carries.
 *
 * <ul>
 *   <li>{@code NUMERIC} — a measured number flagged against numeric reference
 *       bounds (e.g. Haemoglobin 13.4 g/dL).</li>
 *   <li>{@code TEXT} — a free / qualitative value, optionally compared against
 *       an expected normal text (e.g. "Negative", a blood group).</li>
 * </ul>
 */
public enum AnalyteValueKind {
    NUMERIC,
    TEXT
}
