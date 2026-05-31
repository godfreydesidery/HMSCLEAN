package com.otapp.hmis.engine.encounter.result.domain;

import com.otapp.hmis.engine.masterdata.labtest.domain.AnalyteValueKind;
import java.math.BigDecimal;

/**
 * Pure, deterministic abnormal-flagging of a measured lab value against a
 * (snapshotted) reference range. Uses {@link BigDecimal#compareTo} — never
 * floating point. No state, no I/O: trivially unit-testable and reproducible,
 * which is what lets a finalized result's flag be frozen with its snapshot.
 *
 * <p>Critical (panic) bounds take precedence over the normal range; if only
 * some bounds are defined, the value is judged against whatever is present.
 */
public final class LabResultFlagger {

    private LabResultFlagger() {}

    public static LabResultFlag flag(AnalyteValueKind valueKind,
                                     BigDecimal value, String valueText,
                                     BigDecimal refLow, BigDecimal refHigh,
                                     BigDecimal criticalLow, BigDecimal criticalHigh,
                                     String normalText) {
        if (valueKind == AnalyteValueKind.TEXT) {
            if (normalText == null || valueText == null || valueText.isBlank()) {
                return LabResultFlag.NONE;
            }
            return normalText.trim().equalsIgnoreCase(valueText.trim())
                    ? LabResultFlag.NORMAL : LabResultFlag.ABNORMAL;
        }

        // NUMERIC
        if (value == null) return LabResultFlag.NONE;
        if (criticalLow != null && value.compareTo(criticalLow) < 0) return LabResultFlag.CRITICAL_LOW;
        if (criticalHigh != null && value.compareTo(criticalHigh) > 0) return LabResultFlag.CRITICAL_HIGH;
        if (refLow != null && value.compareTo(refLow) < 0) return LabResultFlag.LOW;
        if (refHigh != null && value.compareTo(refHigh) > 0) return LabResultFlag.HIGH;

        boolean anyBound = refLow != null || refHigh != null || criticalLow != null || criticalHigh != null;
        return anyBound ? LabResultFlag.NORMAL : LabResultFlag.NONE;
    }
}
