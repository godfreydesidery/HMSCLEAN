package com.otapp.hmis.engine.masterdata.externalprovider.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An external medical facility a patient can be referred out to (legacy
 * {@code ExternalMedicalProvider}). A flat contact-card master — code + name
 * plus contact details — referenced by closure plans of kind REFERRAL via the
 * plan's {@code externalProviderUid}.
 */
@Entity
@Table(name = "md_external_medical_provider",
       uniqueConstraints = @UniqueConstraint(name = "uk_md_external_medical_provider_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalMedicalProvider extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32) private String code;

    @Setter @Column(nullable = false, length = 200) private String name;
    @Setter @Column(length = 255) private String address;
    @Setter @Column(length = 40)  private String telephone;
    @Setter @Column(length = 120) private String email;
    @Setter @Column(length = 40)  private String fax;
    @Setter @Column(length = 200) private String website;
    @Setter @Column(nullable = false) private boolean active = true;

    public ExternalMedicalProvider(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
