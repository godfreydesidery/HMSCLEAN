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

    /**
     * Denormalised payment gate: TRUE once the consultation fee is settled.
     * Set by the billing-side settlement dispatcher (billing → encounter) when
     * the CONSULTATION invoice is paid in full, or at booking when the invoice
     * is zero-amount (follow-up / plan waiver). Non-CASH consultations are
     * treated as settled by the reception queue / open gate regardless of this
     * flag (legacy "COVERED"). The encounter module never reads billing.
     */
    @Column(name = "fee_settled", nullable = false) private boolean feeSettled = false;
    @Setter @Column(name = "fee_settled_at") private Instant feeSettledAt;

    @Column(name = "booked_at",  nullable = false) private Instant bookedAt;
    @Setter @Column(name = "started_at")   private Instant startedAt;
    @Setter @Column(name = "completed_at") private Instant completedAt;
    /** Legacy SIGNED-OUT timestamp — distinct from generic completion; set on free/sign-out. */
    @Setter @Column(name = "signed_out_at") private Instant signedOutAt;
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

    /** Idempotent — flags the consultation fee as settled. Safe to call repeatedly. */
    public void markFeeSettled() {
        if (!feeSettled) {
            feeSettled = true;
            feeSettledAt = Instant.now();
        }
    }

    public void start() {
        if (status != ConsultationStatus.BOOKED) {
            throw new BusinessRuleException("Only BOOKED consultations can be started (current: " + status + ")");
        }
        status = ConsultationStatus.IN_PROGRESS;
        startedAt = Instant.now();
    }

    /**
     * Clinical-authoring gate (legacy {@code open_consultation} confinement):
     * clinical entries — notes, diagnoses, lab/radiology/procedure orders, and
     * prescriptions — may be authored ONLY while the consultation is
     * IN_PROGRESS (legacy IN-PROCESS). A BOOKED / COMPLETED / CANCELLED /
     * TRANSFERRED consultation rejects writes. Throws {@link BusinessRuleException}
     * (422) on violation.
     */
    public void requireAuthorable() {
        if (status != ConsultationStatus.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "Clinical entries are only allowed while the consultation is IN_PROGRESS (current: " + status + ")");
        }
    }

    public void complete() {
        if (status != ConsultationStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Only IN_PROGRESS consultations can be completed (current: " + status + ")");
        }
        status = ConsultationStatus.COMPLETED;
        completedAt = Instant.now();
        // Legacy sign-out (free_consultation) stamps the SIGNED-OUT moment so the
        // downstream-cancel sweep and audit can distinguish it from a transfer-close.
        signedOutAt = Instant.now();
    }

    /**
     * Cancel a not-yet-started consultation. Faithful to legacy
     * {@code cancel_consultation} ("only a PENDING consultation can be
     * canceled"): only a BOOKED (legacy PENDING) consultation may be cancelled.
     * Once it is IN_PROGRESS the clinician closes it via sign-out
     * ({@link #complete()}), and COMPLETED / TRANSFERRED are terminal. Idempotent
     * on an already-CANCELLED consultation so a retried cancel event is a no-op.
     */
    public void cancel(String reason) {
        if (status == ConsultationStatus.CANCELLED) {
            return; // idempotent — already cancelled
        }
        if (status != ConsultationStatus.BOOKED) {
            throw new BusinessRuleException(
                    "Only a BOOKED consultation can be cancelled (legacy: PENDING) — current: " + status);
        }
        status = ConsultationStatus.CANCELLED;
        cancelledAt = Instant.now();
        cancelReason = reason;
    }

    /**
     * Raise a <em>pending</em> transfer (OPC-1): the active consultation closes as
     * TRANSFERRED without a receiver yet — reception books the receiving
     * consultation later and the link is filled in via {@link #linkTransferTarget}.
     * Legacy two-phase hand-off; only an active (IN_PROGRESS) consultation can be
     * handed off.
     */
    public void markTransferredPending(String reason) {
        if (status != ConsultationStatus.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "Only an IN_PROGRESS consultation can be transferred (current: " + status + ")");
        }
        status = ConsultationStatus.TRANSFERRED;
        transferReason = reason;
        transferredAt = Instant.now();
    }

    /**
     * Fill in the receiver link once reception has booked the fresh consultation
     * for a pending transfer. The source is already TRANSFERRED with no receiver.
     */
    public void linkTransferTarget(String newConsultationUid) {
        if (status != ConsultationStatus.TRANSFERRED || transferredToConsultationUid != null) {
            throw new BusinessRuleException(
                    "Transfer target can only be linked on a TRANSFERRED consultation with no receiver yet");
        }
        transferredToConsultationUid = newConsultationUid;
    }

    /**
     * Revert a pending transfer (OPC-1): the initiating doctor cancelled the
     * request before pickup, so the consultation returns to IN_PROGRESS. Only a
     * TRANSFERRED consultation that has not yet been picked up (no receiver) can be
     * reverted.
     */
    public void revertTransfer() {
        if (status != ConsultationStatus.TRANSFERRED || transferredToConsultationUid != null) {
            throw new BusinessRuleException(
                    "Only a pending (not-yet-accepted) transfer can be reverted (current: " + status + ")");
        }
        status = ConsultationStatus.IN_PROGRESS;
        transferReason = null;
        transferredAt = null;
    }

    /**
     * Outpatient closure — patient died during the encounter. Driven by an
     * approved DECEASED closure plan (legacy DeceasedNote on a consultation).
     * Only an open (IN_PROGRESS) consultation can be closed this way; terminal
     * thereafter. Stamps the sign-out moment like a normal close.
     */
    public void closeAsDeceased() {
        requireOpenForClosure();
        status = ConsultationStatus.DECEASED;
        completedAt = Instant.now();
        signedOutAt = Instant.now();
    }

    /**
     * Outpatient closure — patient referred out to an external facility. Driven
     * by an approved REFERRAL closure plan (legacy ReferralPlan on a
     * consultation). Only an open (IN_PROGRESS) consultation can be closed this
     * way; terminal thereafter.
     */
    public void closeAsReferred() {
        requireOpenForClosure();
        status = ConsultationStatus.REFERRED;
        completedAt = Instant.now();
        signedOutAt = Instant.now();
    }

    private void requireOpenForClosure() {
        if (status != ConsultationStatus.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "Only an IN_PROGRESS consultation can be closed as deceased / referred (current: " + status + ")");
        }
    }
}
