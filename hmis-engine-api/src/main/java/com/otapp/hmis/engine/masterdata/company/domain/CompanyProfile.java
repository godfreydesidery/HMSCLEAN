package com.otapp.hmis.engine.masterdata.company.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Singleton — there is exactly one CompanyProfile row in the hospital
 * (id always 1). Lookup goes through the service which returns the
 * single row or constructs a defaults stub if it hasn't been seeded yet.
 */
@Entity
@Table(name = "md_company_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyProfile extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter @Column(nullable = false, length = 160) private String name;
    @Setter @Column(length = 80)                     private String shortName;
    @Setter @Column(length = 255)                    private String address;
    @Setter @Column(length = 80)                     private String city;
    @Setter @Column(length = 80)                     private String country;
    @Setter @Column(length = 32)                     private String phone;
    @Setter @Column(length = 120)                    private String email;
    @Setter @Column(length = 120)                    private String website;
    @Setter @Column(name = "tax_id", length = 64)    private String taxId;
    @Setter @Column(length = 500)                    private String motto;
    @Setter @Column(name = "logo_url", length = 500) private String logoUrl;
    @Setter @Column(nullable = false, length = 3)    private String currency = "TZS";
    @Setter @Column(length = 64)                     private String timezone;

    public CompanyProfile(String name) {
        this.name = name;
    }
}
