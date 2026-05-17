package com.otapp.hmis.engine.masterdata.clinic.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "md_clinic", uniqueConstraints = @UniqueConstraint(name = "uk_md_clinic_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clinic extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String code;

    @Setter
    @Column(nullable = false, length = 120)
    private String name;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ClinicType type;

    @Setter
    @Column(length = 500)
    private String description;

    @Setter
    @Column(length = 80)
    private String location;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    public Clinic(String code, String name, ClinicType type, String description, String location) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.description = description;
        this.location = location;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
