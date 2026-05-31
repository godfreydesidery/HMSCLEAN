package com.otapp.hmis.engine.encounter.vitals.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Pure, deterministic derivation of BMI and BSA from weight and height. No
 * state, no I/O: trivially unit-testable and reproducible. Uses
 * {@link BigDecimal} (never floating point) for the stored measured values,
 * matching the no-float rule for clinical numerics.
 *
 * <p>Legacy Zana-HMIS computed these client-side and stored them as free-text
 * Strings on {@code GeneralExamination}. The rewrite recomputes them
 * server-side as a convenience when the clinician did not supply a value but
 * both weight and height are present (additive — a submitted value always
 * wins).
 */
public final class VitalsCalculator {

    private VitalsCalculator() {}

    /**
     * BMI = weight(kg) / height(m)^2, rounded to one decimal place (NUMERIC(4,1)).
     * Returns {@code null} if either input is missing or height is non-positive.
     */
    public static BigDecimal bmi(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null) return null;
        if (heightCm.signum() <= 0 || weightKg.signum() <= 0) return null;
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), MathContext.DECIMAL64);
        BigDecimal denom = heightM.multiply(heightM, MathContext.DECIMAL64);
        if (denom.signum() == 0) return null;
        return weightKg.divide(denom, MathContext.DECIMAL64).setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * BSA (Mosteller) = sqrt(height(cm) * weight(kg) / 3600), in m^2, rounded to
     * two decimal places (NUMERIC(4,2)). Returns {@code null} if either input is
     * missing or non-positive.
     */
    public static BigDecimal bsaMosteller(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null) return null;
        if (heightCm.signum() <= 0 || weightKg.signum() <= 0) return null;
        BigDecimal product = heightCm.multiply(weightKg, MathContext.DECIMAL64)
                .divide(BigDecimal.valueOf(3600), MathContext.DECIMAL64);
        BigDecimal root = product.sqrt(MathContext.DECIMAL64);
        return root.setScale(2, RoundingMode.HALF_UP);
    }
}
