package com.otapp.hmis.engine.encounter.consultation.domain;

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
@Table(name = "consultation",
       uniqueConstraints = @UniqueConstraint(name = "uk_consultation_no", columnNames = "consultation_no"),
       indexes = {
               @Index(name = "idx_consultation_patient",   columnList = "patient_uid"),
               @Index(name = "idx_consultation_clinician", columnList = "clinician_username"),
               @Index(name = "idx_consultation_status",    columnList = "status"),
               @Index(name = "idx_consultation_started",   columnList = "started_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consultation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable consultation number, e.g. CN-2026-000123. */
    @Column(name = "consultation_no", nullable = false, length = 32)
    private String consultationNo;

    /** Loose links to other aggregates by their public uid. */
    @Column(name = "patient_uid",   nullable = false, length = 26) private String patientUid;
    @Column(name = "clinic_uid",    nullable = false, length = 26) private String clinicUid;

    /** Username of the clinician (a User in the iam module). */
    @Setter
    @Column(name = "clinician_username", nullable = false, length = 64)
    private String clinicianUsername;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConsultationStatus status = ConsultationStatus.BOOKED;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 16)
    private PaymentType paymentType;

    @Setter @Column(name = "insurance_plan_uid", length = 26) private String insurancePlanUid;

    @Setter @Column(length = 500) private String reason;

    // ----- Phase 44 linkages ----------------------------------------------
    /** When set, this visit is a follow-up to the referenced prior consultation. */
    @Setter @Column(name = "follow_up_of_consultation_uid", length = 26)
    private String followUpOfConsultationUid;

    /** Set on the original consultation when it was transferred to another clinic. */
    @Setter @Column(name = "transferred_to_consultation_uid", length = 26)
    private String transferredToConsultationUid;

    /** Set on the new consultation that took over after a transfer. */
    @Setter @Column(name = "transferred_from_consultation_uid", length = 26)
    private String transferredFromConsultationUid;

    @Setter @Column(name = "transfer_reason", length = 500) private String transferReason;
    @Setter @Column(name = "transferred_at")                private Instant transferredAt;

    @Column(name = "booked_at",  nullable = false) private Instant bookedAt;
    @Setter @Column(name = "started_at")   private Instant startedAt;
    @Setter @Column(name = "completed_at") private Instant completedAt;
    @Setter @Column(name = "cancelled_at") private Instant cancelledAt;
    @Setter @Column(name = "cancel_reason", length = 255) private String cancelReason;

    public Consultation(String consultationNo, String patientUid, String clinicUid,
                        String clinicianUsername, PaymentType paymentType,
                        String insurancePlanUid, String reason) {
        this.consultationNo = consultationNo;
        this.patientUid = patientUid;
        this.clinicUid = clinicUid;
        this.clinicianUsername = clinicianUsername;
        this.paymentType = paymentType;
        this.insurancePlanUid = insurancePlanUid;
        this.reason = reason;
        this.bookedAt = Instant.now();
    }

    public void start() {
        if (status != ConsultationStatus.BOOKED) {
            throw new BusinessRuleException("Only BOOKED consultations can be started (current: " + status + ")");
        }
        status = ConsultationStatus.IN_PROGRESS;
        startedAt = Instant.now();
    }

    public void complete() {
        if (status != ConsultationStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Only IN_PROGRESS consultations can be completed (current: " + status + ")");
        }
        status = ConsultationStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (status == ConsultationStatus.COMPLETED) {
            throw new BusinessRuleException("Completed consultations cannot be cancelled");
        }
        if (status == ConsultationStatus.CANCELLED) {
            return;
        }
        status = ConsultationStatus.CANCELLED;
        cancelledAt = Instant.now();
        cancelReason = reason;
    }

    /** Hand the patient off to {@code newConsultationUid}; this consultation closes as TRANSFERRED. */
    public void markTransferredTo(String newConsultationUid, String reason) {
        if (status != ConsultationStatus.BOOKED && status != ConsultationStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Only BOOKED or IN_PROGRESS consultations can be transferred (current: " + status + ")");
        }
        status = ConsultationStatus.TRANSFERRED;
        transferredToConsultationUid = newConsultationUid;
        transferReason = reason;
        transferredAt = Instant.now();
    }
}
