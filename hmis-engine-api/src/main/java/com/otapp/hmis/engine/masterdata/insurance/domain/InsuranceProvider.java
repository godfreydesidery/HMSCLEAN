package com.otapp.hmis.engine.masterdata.insurance.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "md_insurance_provider", uniqueConstraints = @UniqueConstraint(name = "uk_md_insurance_provider_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsuranceProvider extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32) private String code;

    @Setter @Column(nullable = false, length = 200) private String name;
    @Setter @Column(name = "contact_person", length = 120) private String contactPerson;
    @Setter @Column(length = 40)  private String phone;
    @Setter @Column(length = 120) private String email;
    @Setter @Column(length = 255) private String address;
    @Setter @Column(length = 500) private String description;
    @Setter @Column(nullable = false) private boolean active = true;

    public InsuranceProvider(String code, String name, String contactPerson, String phone,
                             String email, String address, String description) {
        this.code = code;
        this.name = name;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.description = description;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
