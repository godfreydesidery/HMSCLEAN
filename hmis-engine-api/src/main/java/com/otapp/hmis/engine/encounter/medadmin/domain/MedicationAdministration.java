package com.otapp.hmis.engine.encounter.medadmin.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A bedside medication-administration record (the nursing MAR — legacy
 * {@code PatientPrescriptionChart}, PROCESS_MISMATCHES.md M15). Each row is a
 * single dose a nurse actually gave during an admission: the dose given, route,
 * patient response, and when + by whom. Distinct from the pharmacy
 * {@code dispensedAt} (when stock left the pharmacy). Append-only.
 */
@Entity
@Table(name = "medication_administration",
       indexes = {
               @Index(name = "idx_med_admin_admission",    columnList = "admission_uid"),
               @Index(name = "idx_med_admin_prescription", columnList = "prescription_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationAdministration extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_uid",    nullable = false, length = 26) private String admissionUid;
    @Column(name = "prescription_uid", nullable = false, length = 26) private String prescriptionUid;
    @Column(name = "patient_uid",      nullable = false, length = 26) private String patientUid;
    @Column(name = "medicine_uid",     length = 26) private String medicineUid;

    @Column(name = "dose_given",       nullable = false, length = 120) private String doseGiven;
    @Column(name = "route",            length = 80)  private String route;
    @Column(name = "patient_response", length = 500) private String patientResponse;
    @Column(name = "notes",            length = 1000) private String notes;

    @Column(name = "administered_at",            nullable = false) private Instant administeredAt;
    @Column(name = "administered_by_username",   nullable = false, length = 64) private String administeredByUsername;

    @SuppressWarnings("java:S107") // a single immutable record; the fields are all part of one chart entry
    public MedicationAdministration(String admissionUid, String prescriptionUid, String patientUid,
                                    String medicineUid, String doseGiven, String route,
                                    String patientResponse, String notes,
                                    Instant administeredAt, String administeredByUsername) {
        this.admissionUid = admissionUid;
        this.prescriptionUid = prescriptionUid;
        this.patientUid = patientUid;
        this.medicineUid = medicineUid;
        this.doseGiven = doseGiven;
        this.route = route;
        this.patientResponse = patientResponse;
        this.notes = notes;
        this.administeredAt = administeredAt;
        this.administeredByUsername = administeredByUsername;
    }
}
