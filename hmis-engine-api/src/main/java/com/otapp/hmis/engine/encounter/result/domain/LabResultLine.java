package com.otapp.hmis.engine.encounter.result.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import com.otapp.hmis.engine.masterdata.labtest.domain.AnalyteValueKind;
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

/**
 * One measured analyte value on a lab {@link OrderResult}. Created only for
 * LAB_TEST orders; radiology/procedure keep using the narrative-only result.
 *
 * <p>The analyte definition (code/name/unit/valueKind) and the applicable
 * reference range (bounds + display) are <strong>snapshotted</strong> at entry
 * time, and the {@link #flag} is computed then — so a later masterdata edit can
 * never retro-change a recorded/signed result, and result reads never reach
 * back into masterdata. Lines are rebuilt wholesale on each preliminary save /
 * amend, so the row is effectively immutable once written.
 */
@Entity
@Table(name = "lab_result_line",
       uniqueConstraints = {
               @UniqueConstraint(name = "uk_lab_result_line_uid", columnNames = "uid"),
               @UniqueConstraint(name = "uk_lab_result_line_result_analyte",
                                 columnNames = {"order_result_uid", "analyte_uid"})
       },
       indexes = @Index(name = "idx_lab_result_line_result", columnList = "order_result_uid"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LabResultLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_result_uid", nullable = false, length = 26) private String orderResultUid;
    @Column(name = "analyte_uid",       nullable = false, length = 26) private String analyteUid;

    // ---- snapshot of the analyte definition ----
    @Column(name = "analyte_code", nullable = false, length = 32)  private String analyteCode;
    @Column(name = "analyte_name", nullable = false, length = 120) private String analyteName;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_kind", nullable = false, length = 16) private AnalyteValueKind valueKind;

    @Column(length = 32) private String unit;

    // ---- the measured value ----
    @Column(name = "value_numeric", precision = 14, scale = 4) private BigDecimal valueNumeric;
    @Column(name = "value_text", length = 2000)                private String valueText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16) private LabResultFlag flag = LabResultFlag.NONE;

    // ---- snapshot of the applicable reference range ----
    @Column(name = "ref_low",       precision = 14, scale = 4) private BigDecimal refLow;
    @Column(name = "ref_high",      precision = 14, scale = 4) private BigDecimal refHigh;
    @Column(name = "critical_low",  precision = 14, scale = 4) private BigDecimal criticalLow;
    @Column(name = "critical_high", precision = 14, scale = 4) private BigDecimal criticalHigh;
    @Column(name = "range_normal_text",   length = 200) private String rangeNormalText;
    @Column(name = "range_display",       length = 120) private String rangeDisplay;
    @Column(name = "reference_range_uid", length = 26)  private String referenceRangeUid;

    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(length = 500)                             private String note;

    @SuppressWarnings("java:S107") // snapshot entity legitimately captures many fields at once
    public LabResultLine(String orderResultUid, String analyteUid, String analyteCode, String analyteName,
                         AnalyteValueKind valueKind, String unit, BigDecimal valueNumeric, String valueText,
                         LabResultFlag flag, BigDecimal refLow, BigDecimal refHigh,
                         BigDecimal criticalLow, BigDecimal criticalHigh, String rangeNormalText,
                         String rangeDisplay, String referenceRangeUid, int displayOrder, String note) {
        this.orderResultUid = orderResultUid;
        this.analyteUid = analyteUid;
        this.analyteCode = analyteCode;
        this.analyteName = analyteName;
        this.valueKind = valueKind;
        this.unit = unit;
        this.valueNumeric = valueNumeric;
        this.valueText = valueText;
        this.flag = flag == null ? LabResultFlag.NONE : flag;
        this.refLow = refLow;
        this.refHigh = refHigh;
        this.criticalLow = criticalLow;
        this.criticalHigh = criticalHigh;
        this.rangeNormalText = rangeNormalText;
        this.rangeDisplay = rangeDisplay;
        this.referenceRangeUid = referenceRangeUid;
        this.displayOrder = displayOrder;
        this.note = note;
    }
}
