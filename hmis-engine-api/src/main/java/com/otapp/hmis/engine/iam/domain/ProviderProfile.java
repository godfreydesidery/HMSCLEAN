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
 * Optional clinical-identity sidecar for a {@link User}, keyed by the user's
 * {@code uid} (1:1). Restores the legacy {@code Clinician.type} (specialty) and
 * registration / licence number without polluting {@code iam_user} with columns
 * that only apply to clinical staff.
 */
@Entity
@Table(name = "iam_provider_profile",
        uniqueConstraints = @UniqueConstraint(name = "uk_iam_provider_profile_user", columnNames = "user_uid"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProviderProfile extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uid", nullable = false, length = 26)
    private String userUid;

    @Setter
    @Column(length = 64)
    private String specialty;

    @Setter
    @Column(name = "registration_no", length = 64)
    private String registrationNo;

    @Setter
    @Column(name = "license_no", length = 64)
    private String licenseNo;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    public ProviderProfile(String userUid, String specialty, String registrationNo, String licenseNo) {
        this.userUid = userUid;
        this.specialty = specialty;
        this.registrationNo = registrationNo;
        this.licenseNo = licenseNo;
    }
}
