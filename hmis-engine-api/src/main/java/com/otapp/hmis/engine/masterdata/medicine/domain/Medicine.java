package com.otapp.hmis.engine.masterdata.medicine.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "md_medicine", uniqueConstraints = @UniqueConstraint(name = "uk_md_medicine_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medicine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32) private String code;

    @Setter @Column(nullable = false, length = 200) private String name;
    @Setter @Column(name = "generic_name", length = 200) private String genericName;
    @Setter @Column(length = 80) private String strength;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "form", nullable = false, length = 32)
    private MedicineForm form;

    @Setter @Column(length = 500) private String description;
    @Setter @Column(nullable = false) private boolean active = true;

    public Medicine(String code, String name, String genericName, String strength,
                    MedicineForm form, String description) {
        this.code = code;
        this.name = name;
        this.genericName = genericName;
        this.strength = strength;
        this.form = form;
        this.description = description;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
