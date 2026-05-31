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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single named, typed result line of a lab test type (panel) — e.g. WBC,
 * HGB and PLT analytes of a Complete Blood Count, each with its own unit and
 * reference ranges. A simple single-value test (e.g. fasting glucose) is just
 * a panel with one analyte.
 *
 * <p>This is the clean replacement for the legacy {@code LabTestTypeRange},
 * which was a bare name-only string. The parent is referenced by its public
 * {@code uid} (loose coupling — same convention as {@code MedicineUnit}).
 */
@Entity
@Table(name = "md_lab_test_analyte",
       uniqueConstraints = @UniqueConstraint(name = "uk_md_lab_test_analyte_type_code",
                                             columnNames = {"lab_test_type_uid", "code"}),
       indexes = @Index(name = "idx_md_lab_test_analyte_type", columnList = "lab_test_type_uid"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LabTestAnalyte extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lab_test_type_uid", nullable = false, length = 26)
    private String labTestTypeUid;

    /** Analyte mnemonic, e.g. {@code WBC}. Uppercase, unique within the panel. */
    @Column(nullable = false, length = 32)
    private String code;

    @Setter @Column(nullable = false, length = 120) private String name;
    @Setter @Column(length = 32)                    private String unit;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "value_kind", nullable = false, length = 16)
    private AnalyteValueKind valueKind = AnalyteValueKind.NUMERIC;

    @Setter @Column(name = "display_order", nullable = false) private int displayOrder;
    @Setter @Column(nullable = false)                         private boolean active = true;

    public LabTestAnalyte(String labTestTypeUid, String code, String name, String unit,
                          AnalyteValueKind valueKind, int displayOrder) {
        this.labTestTypeUid = labTestTypeUid;
        this.code = code.trim().toUpperCase();
        this.name = name;
        this.unit = unit;
        this.valueKind = valueKind == null ? AnalyteValueKind.NUMERIC : valueKind;
        this.displayOrder = displayOrder;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
