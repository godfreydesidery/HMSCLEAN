package com.otapp.hmis.engine.masterdata.insurance.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "md_insurance_plan", uniqueConstraints = {
        @UniqueConstraint(name = "uk_md_insurance_plan_code", columnNames = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsurancePlan extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32) private String code;

    @Setter @Column(nullable = false, length = 200) private String name;

    /**
     * Stored as the provider's public uid to keep the persistence-level coupling
     * to the InsuranceProvider aggregate explicit but loose (no JPA-managed FK
     * to the other aggregate).
     */
    @Column(name = "provider_uid", nullable = false, length = 26)
    private String providerUid;

    @Setter @Column(name = "covers_consultation", nullable = false) private boolean coversConsultation = true;
    @Setter @Column(name = "covers_lab",          nullable = false) private boolean coversLab          = true;
    @Setter @Column(name = "covers_radiology",    nullable = false) private boolean coversRadiology    = true;
    @Setter @Column(name = "covers_procedure",    nullable = false) private boolean coversProcedure    = true;
    @Setter @Column(name = "covers_medicine",     nullable = false) private boolean coversMedicine     = true;
    @Setter @Column(name = "covers_admission",    nullable = false) private boolean coversAdmission    = true;

    @Setter @Column(length = 500) private String description;
    @Setter @Column(nullable = false) private boolean active = true;

    public InsurancePlan(String code, String name, String providerUid, String description) {
        this.code = code;
        this.name = name;
        this.providerUid = providerUid;
        this.description = description;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
