package com.otapp.hmis.engine.encounter.result.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.masterdata.labtest.domain.AnalyteValueKind;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Pure unit coverage of the deterministic abnormal-flagging logic. No Spring,
 * no DB — just the {@link LabResultFlagger} decision table, including the
 * boundary cases that are easy to get wrong (equality at a bound, partial
 * bounds, panic precedence, missing bounds, text comparison).
 */
class LabResultFlaggerTest {

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    private static LabResultFlag numeric(String value, String refLow, String refHigh, String critLow, String critHigh) {
        return LabResultFlagger.flag(AnalyteValueKind.NUMERIC, value == null ? null : bd(value), null,
                refLow == null ? null : bd(refLow), refHigh == null ? null : bd(refHigh),
                critLow == null ? null : bd(critLow), critHigh == null ? null : bd(critHigh), null);
    }

    @Test
    void numericWithinRangeIsNormal() {
        assertThat(numeric("7.0", "4.0", "11.0", "1.0", "30.0")).isEqualTo(LabResultFlag.NORMAL);
    }

    @Test
    void numericBelowRefLowIsLow() {
        assertThat(numeric("3.0", "4.0", "11.0", "1.0", "30.0")).isEqualTo(LabResultFlag.LOW);
    }

    @Test
    void numericAboveRefHighIsHigh() {
        assertThat(numeric("12.0", "4.0", "11.0", "1.0", "30.0")).isEqualTo(LabResultFlag.HIGH);
    }

    @Test
    void numericBelowCriticalLowIsCriticalLow() {
        assertThat(numeric("0.5", "4.0", "11.0", "1.0", "30.0")).isEqualTo(LabResultFlag.CRITICAL_LOW);
    }

    @Test
    void numericAboveCriticalHighIsCriticalHigh() {
        assertThat(numeric("31.0", "4.0", "11.0", "1.0", "30.0")).isEqualTo(LabResultFlag.CRITICAL_HIGH);
    }

    @Test
    void valueExactlyAtRefLowIsNormalNotLow() {
        assertThat(numeric("4.0", "4.0", "11.0", null, null)).isEqualTo(LabResultFlag.NORMAL);
    }

    @Test
    void valueExactlyAtRefHighIsNormalNotHigh() {
        assertThat(numeric("11.0", "4.0", "11.0", null, null)).isEqualTo(LabResultFlag.NORMAL);
    }

    @Test
    void criticalTakesPrecedenceOverNormalLow() {
        // value below both refLow and criticalLow -> the critical verdict wins
        assertThat(numeric("0.9", "4.0", "11.0", "1.0", "30.0")).isEqualTo(LabResultFlag.CRITICAL_LOW);
    }

    @Test
    void onlyUpperBoundDefined() {
        assertThat(numeric("50", null, "100", null, null)).isEqualTo(LabResultFlag.NORMAL);
        assertThat(numeric("150", null, "100", null, null)).isEqualTo(LabResultFlag.HIGH);
    }

    @Test
    void noBoundsAtAllIsNone() {
        assertThat(numeric("7.0", null, null, null, null)).isEqualTo(LabResultFlag.NONE);
    }

    @Test
    void nullNumericValueIsNone() {
        assertThat(numeric(null, "4.0", "11.0", null, null)).isEqualTo(LabResultFlag.NONE);
    }

    @Test
    void textMatchingNormalIsNormalCaseInsensitive() {
        assertThat(LabResultFlagger.flag(AnalyteValueKind.TEXT, null, "negative", null, null, null, null, "Negative"))
                .isEqualTo(LabResultFlag.NORMAL);
    }

    @Test
    void textNotMatchingNormalIsAbnormal() {
        assertThat(LabResultFlagger.flag(AnalyteValueKind.TEXT, null, "Positive", null, null, null, null, "Negative"))
                .isEqualTo(LabResultFlag.ABNORMAL);
    }

    @Test
    void textWithNoExpectedNormalIsNone() {
        assertThat(LabResultFlagger.flag(AnalyteValueKind.TEXT, null, "Positive", null, null, null, null, null))
                .isEqualTo(LabResultFlag.NONE);
    }

    @Test
    void textBlankValueIsNone() {
        assertThat(LabResultFlagger.flag(AnalyteValueKind.TEXT, null, "  ", null, null, null, null, "Negative"))
                .isEqualTo(LabResultFlag.NONE);
    }
}
