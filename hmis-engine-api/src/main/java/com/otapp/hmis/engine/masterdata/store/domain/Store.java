package com.otapp.hmis.engine.masterdata.store.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "md_store", uniqueConstraints = @UniqueConstraint(name = "uk_md_store_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32) private String code;

    @Setter @Column(nullable = false, length = 120) private String name;
    @Setter @Column(length = 80)                   private String location;
    @Setter @Column(length = 500)                  private String description;
    @Setter @Column(nullable = false)              private boolean active = true;

    public Store(String code, String name, String location, String description) {
        this.code = code;
        this.name = name;
        this.location = location;
        this.description = description;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
