package com.otapp.hmis.engine.encounter.consultation.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * A clinic-to-clinic consultation transfer request (legacy Zana-HMIS two-phase
 * hand-off). A sibling aggregate to {@link Consultation}: the treating doctor
 * raises it against a target <em>clinic</em> only (the receiving clinician is
 * chosen later at pickup), and reception completes it by booking a fresh
 * consultation at that clinic.
 *
 * <p>Cross-aggregate references are by public {@code uid} per project convention.
 */
@Entity
@Table(name = "consultation_transfer",
       indexes = {
               @Index(name = "idx_consultation_transfer_status",  columnList = "status"),
               @Index(name = "idx_consultation_transfer_patient", columnList = "patient_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationTransfer extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The consultation the patient is being handed off FROM. */
    @Column(name = "source_consultation_uid", nullable = false, length = 26)
    private String sourceConsultationUid;

    @Column(name = "patient_uid", nullable = false, length = 26)
    private String patientUid;

    /** Target CLINIC only — the receiving clinician is chosen at pickup. */
    @Column(name = "target_clinic_uid", nullable = false, length = 26)
    private String targetClinicUid;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ConsultationTransferStatus status = ConsultationTransferStatus.PENDING;

    /** Set on COMPLETED — the fresh consultation reception booked at the target clinic. */
    @Column(name = "created_consultation_uid", length = 26)
    private String createdConsultationUid;

    /** Optional note recorded when the initiating doctor reverts the request. */
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public ConsultationTransfer(String sourceConsultationUid, String patientUid,
                                String targetClinicUid, String reason) {
        this.sourceConsultationUid = sourceConsultationUid;
        this.patientUid = patientUid;
        this.targetClinicUid = targetClinicUid;
        this.reason = reason;
        this.status = ConsultationTransferStatus.PENDING;
    }

    /**
     * Reception accepted the request and booked {@code newConsultationUid} at the
     * target clinic. Only a PENDING transfer can be completed.
     */
    public void complete(String newConsultationUid) {
        if (status != ConsultationTransferStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only a PENDING transfer can be accepted (current: " + status + ")");
        }
        status = ConsultationTransferStatus.COMPLETED;
        createdConsultationUid = newConsultationUid;
        completedAt = Instant.now();
    }

    /**
     * The initiating doctor reverted the request before pickup. Only a PENDING
     * transfer can be cancelled.
     */
    public void cancel(String reason) {
        if (status != ConsultationTransferStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only a PENDING transfer can be cancelled (current: " + status + ")");
        }
        status = ConsultationTransferStatus.CANCELLED;
        cancelReason = reason;
        cancelledAt = Instant.now();
    }
}
