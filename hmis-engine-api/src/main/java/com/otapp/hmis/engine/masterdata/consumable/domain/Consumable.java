package com.otapp.hmis.engine.masterdata.consumable.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ward / nursing consumable — gauze, IV fluids, syringes, gloves, etc.
 * Sibling masterdata to {@code Medicine} but with no dosing semantics.
 * Future phases will wire consumables into a ward-issue path that feeds
 * the patient consumable chart.
 */
@Entity
@Table(name = "md_consumable",
       uniqueConstraints = @UniqueConstraint(name = "uk_md_consumable_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consumable extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)  private String code;
    @Setter @Column(nullable = false, length = 160) private String name;
    @Setter @Column(name = "unit_of_measure", length = 32) private String unitOfMeasure;
    @Setter @Column(length = 500) private String description;
    @Setter @Column(nullable = false) private boolean active = true;

    public Consumable(String code, String name, String unitOfMeasure, String description) {
        this.code = code;
        this.name = name;
        this.unitOfMeasure = unitOfMeasure;
        this.description = description;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
