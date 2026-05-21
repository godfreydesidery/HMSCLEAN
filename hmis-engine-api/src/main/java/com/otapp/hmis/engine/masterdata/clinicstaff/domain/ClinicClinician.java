package com.otapp.hmis.engine.masterdata.clinicstaff.domain;

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

/**
 * Affiliation row binding a clinician (an {@code iam} {@code User} with the
 * {@code CLINICIAN} role) to a {@link com.otapp.hmis.engine.masterdata.clinic.domain.Clinic}.
 *
 * <p>Restores the legacy {@code Clinician.clinics} many-to-many: a clinician
 * works at one or more clinics, and a consultation may only be booked for a
 * clinician who is affiliated with the chosen clinic.
 *
 * <p>Coupling to {@code iam} is by identity string (the canonical
 * {@code user_uid} plus the {@code username} denormalized, because
 * {@code Consultation} routes by {@code clinicianUsername}) — there is no
 * cross-module database foreign key.
 */
@Entity
@Table(name = "md_clinic_clinician",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_md_clinic_clinician", columnNames = {"clinic_uid", "user_uid"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicClinician extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_uid", nullable = false, length = 26)
    private String clinicUid;

    @Column(name = "user_uid", nullable = false, length = 26)
    private String userUid;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(nullable = false)
    private boolean active = true;

    public ClinicClinician(String clinicUid, String userUid, String username) {
        this.clinicUid = clinicUid;
        this.userUid = userUid;
        this.username = username;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
