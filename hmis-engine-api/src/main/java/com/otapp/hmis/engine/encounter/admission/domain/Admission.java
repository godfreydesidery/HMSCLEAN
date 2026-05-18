package com.otapp.hmis.engine.encounter.admission.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admission",
       uniqueConstraints = @UniqueConstraint(name = "uk_admission_no", columnNames = "admission_no"),
       indexes = {
               @Index(name = "idx_admission_patient",  columnList = "patient_uid"),
               @Index(name = "idx_admission_ward",     columnList = "ward_uid"),
               @Index(name = "idx_admission_status",   columnList = "status"),
               @Index(name = "idx_admission_admitted", columnList = "admitted_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admission extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable admission number, e.g. AD-2026-000123. */
    @Column(name = "admission_no", nullable = false, length = 32)
    private String admissionNo;

    @Column(name = "patient_uid", nullable = false, length = 26) private String patientUid;

    @Setter @Column(name = "ward_uid", nullable = false, length = 26) private String wardUid;
    /** Optional reference to a real {@code Bed} entity. Null = unassigned / free-text fallback. */
    @Setter @Column(name = "bed_uid",   length = 26) private String bedUid;
    /** Denormalised bed label — set from the Bed on assignment, or free text if bedUid is null. */
    @Setter @Column(name = "bed_label", length = 32) private String bedLabel;

    /** Admitting clinician (User in the iam module). */
    @Setter
    @Column(name = "admitting_clinician_username", nullable = false, length = 64)
    private String admittingClinicianUsername;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AdmissionStatus status = AdmissionStatus.ADMITTED;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 16)
    private PaymentType paymentType;

    @Setter @Column(name = "insurance_plan_uid", length = 26) private String insurancePlanUid;

    /** Optional: consultation this admission was triggered from. */
    @Setter @Column(name = "consultation_uid", length = 26) private String consultationUid;

    @Setter @Column(name = "admission_reason", length = 500) private String admissionReason;

    @Column(name = "admitted_at",   nullable = false) private Instant admittedAt;
    @Setter @Column(name = "discharged_at") private Instant dischargedAt;
    @Setter @Column(name = "discharge_summary", length = 1000) private String dischargeSummary;
    @Setter @Column(name = "cancelled_at") private Instant cancelledAt;
    @Setter @Column(name = "cancel_reason", length = 255) private String cancelReason;

    public Admission(String admissionNo, String patientUid, String wardUid, String bedLabel,
                     String admittingClinicianUsername, PaymentType paymentType,
                     String insurancePlanUid, String consultationUid, String admissionReason) {
        this.admissionNo = admissionNo;
        this.patientUid = patientUid;
        this.wardUid = wardUid;
        this.bedLabel = bedLabel;
        this.admittingClinicianUsername = admittingClinicianUsername;
        this.paymentType = paymentType;
        this.insurancePlanUid = insurancePlanUid;
        this.consultationUid = consultationUid;
        this.admissionReason = admissionReason;
        this.admittedAt = Instant.now();
    }

    public void transferWard(String newWardUid, String newBedLabel) {
        if (status != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException("Only ADMITTED patients can be moved (current: " + status + ")");
        }
        this.wardUid = newWardUid;
        this.bedLabel = newBedLabel;
    }

    public void discharge(String summary) {
        if (status != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException("Only ADMITTED patients can be discharged (current: " + status + ")");
        }
        status = AdmissionStatus.DISCHARGED;
        dischargedAt = Instant.now();
        dischargeSummary = summary;
    }

    public void markDeceased(String summary) {
        if (status != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException("Only ADMITTED patients can be marked deceased (current: " + status + ")");
        }
        status = AdmissionStatus.DECEASED;
        dischargedAt = Instant.now();
        dischargeSummary = summary;
    }

    public void transferOut(String summary) {
        if (status != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException("Only ADMITTED patients can be transferred out (current: " + status + ")");
        }
        status = AdmissionStatus.TRANSFERRED;
        dischargedAt = Instant.now();
        dischargeSummary = summary;
    }

    public void cancel(String reason) {
        if (status == AdmissionStatus.CANCELLED) {
            return;
        }
        if (status != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException("Admission can only be cancelled while ADMITTED (current: " + status + ")");
        }
        status = AdmissionStatus.CANCELLED;
        cancelledAt = Instant.now();
        cancelReason = reason;
    }
}
