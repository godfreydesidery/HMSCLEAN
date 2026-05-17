package com.otapp.hmis.engine.masterdata.labtest.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "md_lab_test_type", uniqueConstraints = @UniqueConstraint(name = "uk_md_lab_test_type_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LabTestType extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32) private String code;

    @Setter @Column(nullable = false, length = 200) private String name;
    @Setter @Column(length = 80)                    private String specimen;
    @Setter @Column(length = 32)                    private String unit;
    @Setter @Column(length = 500)                   private String description;
    @Setter @Column(nullable = false)               private boolean active = true;

    public LabTestType(String code, String name, String specimen, String unit, String description) {
        this.code = code;
        this.name = name;
        this.specimen = specimen;
        this.unit = unit;
        this.description = description;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
