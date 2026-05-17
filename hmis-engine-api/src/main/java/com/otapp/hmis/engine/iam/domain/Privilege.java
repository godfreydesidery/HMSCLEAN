package com.otapp.hmis.engine.iam.domain;

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
 * A privilege is a fine-grained permission identified by name, e.g.
 * {@code PATIENT_READ}, {@code PRESCRIPTION_APPROVE}.
 *
 * <p>Privileges are pure data — they are managed via the admin UI, not
 * generated reflectively from code. New privileges are added via Flyway
 * migrations and granted to roles by administrators.
 */
@Entity
@Table(name = "iam_privilege", uniqueConstraints = @UniqueConstraint(name = "uk_iam_privilege_name", columnNames = "name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Privilege extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Setter
    @Column(length = 255)
    private String description;

    public Privilege(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
