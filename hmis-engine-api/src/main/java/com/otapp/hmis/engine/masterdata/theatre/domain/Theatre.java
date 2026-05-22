package com.otapp.hmis.engine.masterdata.theatre.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An operating theatre (PROCESS.md §7, §17.13). Procedure orders that
 * require theatre time book against a specific theatre via
 * {@link com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder#getTheatreUid()}.
 */
@Entity
@Table(name = "md_theatre",
       uniqueConstraints = @UniqueConstraint(name = "uk_md_theatre_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Theatre extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String code;

    @Setter @Column(nullable = false, length = 120) private String name;
    @Setter @Column(length = 80)                     private String location;
    @Setter @Column(length = 500)                    private String description;
    @Setter @Column(nullable = false)                private boolean active = true;

    public Theatre(String code, String name, String location, String description) {
        this.code = code;
        this.name = name;
        this.location = location;
        this.description = description;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
