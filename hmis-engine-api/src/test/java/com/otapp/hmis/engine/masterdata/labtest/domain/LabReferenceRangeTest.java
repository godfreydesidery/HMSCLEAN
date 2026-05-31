package com.otapp.hmis.engine.masterdata.labtest.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit coverage of reference-range applicability — especially the unknown-age
 * policy: a patient whose age cannot be resolved must only match fully
 * open-ended ranges, never an age band (which would otherwise snapshot e.g.
 * neonatal bounds onto an adult result).
 */
class LabReferenceRangeTest {

    private static LabReferenceRange range(RangeSex sex, Integer ageMin, Integer ageMax) {
        return new LabReferenceRange("AN1", sex, ageMin, ageMax, null, null, null, null, null, null);
    }

    @Test
    void openEndedRangeMatchesAnyAgeIncludingUnknown() {
        LabReferenceRange r = range(RangeSex.ANY, null, null);
        assertThat(r.appliesTo(RangeSex.MALE, null)).isTrue();
        assertThat(r.appliesTo(RangeSex.MALE, 30)).isTrue();
        assertThat(r.appliesTo(RangeSex.FEMALE, 30000)).isTrue();
    }

    @Test
    void ageBandedRangeDoesNotMatchUnknownAge() {
        LabReferenceRange neonatal = range(RangeSex.ANY, 0, 28);
        assertThat(neonatal.appliesTo(RangeSex.MALE, null)).isFalse();   // unknown age -> no age band
        assertThat(neonatal.appliesTo(RangeSex.MALE, 10)).isTrue();
        assertThat(neonatal.appliesTo(RangeSex.MALE, 29)).isFalse();
    }

    @Test
    void openLowerBoundOnlyStillCountsAsBandedForUnknownAge() {
        LabReferenceRange adult = range(RangeSex.ANY, 6570, null); // >= ~18y
        assertThat(adult.appliesTo(RangeSex.MALE, null)).isFalse();
        assertThat(adult.appliesTo(RangeSex.MALE, 7000)).isTrue();
        assertThat(adult.appliesTo(RangeSex.MALE, 100)).isFalse();
    }

    @Test
    void sexSpecificRangeOnlyMatchesThatSex() {
        LabReferenceRange male = range(RangeSex.MALE, null, null);
        assertThat(male.appliesTo(RangeSex.MALE, 30)).isTrue();
        assertThat(male.appliesTo(RangeSex.FEMALE, 30)).isFalse();
        assertThat(male.appliesTo(RangeSex.ANY, 30)).isFalse();
    }

    @Test
    void inactiveRangeNeverApplies() {
        LabReferenceRange r = range(RangeSex.ANY, null, null);
        r.setActive(false);
        assertThat(r.appliesTo(RangeSex.MALE, 30)).isFalse();
    }
}
