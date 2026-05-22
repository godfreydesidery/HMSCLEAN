package com.otapp.hmis.engine.masterdata.dosage.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standard dosing strength lookup — replaces free-text "dose" entries on
 * prescriptions with picklist values like "1 tablet", "5 ml", "2 puffs"
 * (PROCESS.md §17.13). Prescription wiring is a future polish; today
 * the dropdown just lives in masterdata.
 */
@Entity
@Table(name = "md_dosage",
       uniqueConstraints = @UniqueConstraint(name = "uk_md_dosage_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dosage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)  private String code;
    @Setter @Column(nullable = false, length = 120) private String name;
    @Setter @Column(length = 500)                    private String description;
    @Setter @Column(nullable = false)                private boolean active = true;

    public Dosage(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
