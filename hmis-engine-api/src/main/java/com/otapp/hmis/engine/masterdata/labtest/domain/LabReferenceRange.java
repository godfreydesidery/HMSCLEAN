package com.otapp.hmis.engine.masterdata.labtest.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A reference range for an analyte, banded by sex and (optionally) age.
 * Replaces the legacy free-text "range" string + manual Low/Med/High pick
 * with real numeric bounds the flagger can evaluate deterministically.
 *
 * <p>Age is banded in <em>days</em> for neonatal precision; a null bound is
 * open-ended. {@code criticalLow}/{@code criticalHigh} mark panic values.
 * {@code normalText} is the expected value for {@code TEXT} analytes
 * (e.g. "Negative"). The analyte is referenced by its public {@code uid}.
 */
@Entity
@Table(name = "md_lab_reference_range",
       uniqueConstraints = @UniqueConstraint(name = "uk_md_lab_reference_range_uid", columnNames = "uid"),
       indexes = @Index(name = "idx_md_lab_reference_range_analyte", columnList = "analyte_uid"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LabReferenceRange extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analyte_uid", nullable = false, length = 26)
    private String analyteUid;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private RangeSex sex = RangeSex.ANY;

    @Setter @Column(name = "age_min_days") private Integer ageMinDays;
    @Setter @Column(name = "age_max_days") private Integer ageMaxDays;

    @Setter @Column(name = "ref_low",       precision = 14, scale = 4) private BigDecimal refLow;
    @Setter @Column(name = "ref_high",      precision = 14, scale = 4) private BigDecimal refHigh;
    @Setter @Column(name = "critical_low",  precision = 14, scale = 4) private BigDecimal criticalLow;
    @Setter @Column(name = "critical_high", precision = 14, scale = 4) private BigDecimal criticalHigh;

    @Setter @Column(name = "normal_text",   length = 200) private String normalText;
    @Setter @Column(name = "range_display", length = 120) private String rangeDisplay;
    @Setter @Column(nullable = false)                     private boolean active = true;

    public LabReferenceRange(String analyteUid, RangeSex sex, Integer ageMinDays, Integer ageMaxDays,
                             BigDecimal refLow, BigDecimal refHigh, BigDecimal criticalLow, BigDecimal criticalHigh,
                             String normalText, String rangeDisplay) {
        this.analyteUid = analyteUid;
        this.sex = sex == null ? RangeSex.ANY : sex;
        this.ageMinDays = ageMinDays;
        this.ageMaxDays = ageMaxDays;
        this.refLow = refLow;
        this.refHigh = refHigh;
        this.criticalLow = criticalLow;
        this.criticalHigh = criticalHigh;
        this.normalText = normalText;
        this.rangeDisplay = rangeDisplay;
    }

    /**
     * True when this range applies to a patient of the given sex + age-in-days.
     * When the age is unknown ({@code ageDays == null}) an age-banded range must
     * NOT match — only fully open-ended ("all ages") ranges apply — so a patient
     * with no resolvable age is never silently flagged against, say, a neonatal band.
     */
    public boolean appliesTo(RangeSex patientSex, Integer ageDays) {
        if (!active) return false;
        if (sex != RangeSex.ANY && sex != patientSex) return false;
        boolean ageBanded = ageMinDays != null || ageMaxDays != null;
        if (ageDays == null) {
            return !ageBanded;
        }
        if (ageMinDays != null && ageDays < ageMinDays) return false;
        if (ageMaxDays != null && ageDays > ageMaxDays) return false;
        return true;
    }

    /** Higher = more specific; used to pick the best-matching range. */
    public int specificity() {
        int score = 0;
        if (sex != RangeSex.ANY) score += 2;
        if (ageMinDays != null || ageMaxDays != null) score += 1;
        return score;
    }
}
