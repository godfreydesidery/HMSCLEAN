package com.otapp.hmis.engine.masterdata.administrationroute.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Drug administration route lookup — ORAL, IV, IM, SC, TOPICAL, etc.
 * (PROCESS.md §17.13). Wire into Prescription as a follow-up.
 */
@Entity
@Table(name = "md_administration_route",
       uniqueConstraints = @UniqueConstraint(name = "uk_md_admin_route_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdministrationRoute extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)  private String code;
    @Setter @Column(nullable = false, length = 120) private String name;
    @Setter @Column(length = 500)                    private String description;
    @Setter @Column(nullable = false)                private boolean active = true;

    public AdministrationRoute(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
