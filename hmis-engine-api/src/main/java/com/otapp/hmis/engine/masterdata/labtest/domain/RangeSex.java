package com.otapp.hmis.engine.masterdata.labtest.domain;

/**
 * Sex band a reference range applies to. {@code ANY} matches every patient;
 * {@code MALE}/{@code FEMALE} match a patient of that sex and are preferred
 * over {@code ANY} when both are defined for the same analyte.
 *
 * <p>Kept module-local (masterdata) on purpose — the patient module owns its
 * own richer {@code Gender}; the encounter layer maps Gender → RangeSex when
 * resolving a range so masterdata never depends on the patient module.
 */
public enum RangeSex {
    ANY,
    MALE,
    FEMALE
}
