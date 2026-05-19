package com.otapp.hmis.engine.encounter.prescription.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A medication order raised during a consultation. The link to the
 * masterdata Medicine is by uid; quantity / dose / frequency / duration are
 * captured in free-text-with-structure form.
 */
@Entity
@Table(name = "prescription",
       uniqueConstraints = @UniqueConstraint(name = "uk_prescription_no", columnNames = "prescription_no"),
       indexes = {
               @Index(name = "idx_prescription_consultation", columnList = "consultation_uid"),
               @Index(name = "idx_prescription_status",       columnList = "status"),
               @Index(name = "idx_prescription_patient",      columnList = "patient_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prescription extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_no", nullable = false, length = 32)
    private String prescriptionNo;

    /** Null for OUTSIDER (walk-in) prescriptions raised directly against the patient. */
    @Column(name = "consultation_uid", length = 26) private String consultationUid;
    @Column(name = "patient_uid",      nullable = false, length = 26) private String patientUid;
    @Column(name = "medicine_uid",     nullable = false, length = 26) private String medicineUid;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PrescriptionStatus status = PrescriptionStatus.PENDING;

    @Setter @Column(name = "dose",          nullable = false, length = 80) private String dose;
    @Setter @Column(name = "frequency",     nullable = false, length = 80) private String frequency;
    @Setter @Column(name = "duration_days") private Integer durationDays;
    @Setter @Column(name = "quantity")      private Integer quantity;
    @Setter @Column(name = "instructions",  length = 500)                  private String instructions;

    // ----- masterdata picklist references (Phase 33) -----------------------
    // Optional. When set, the service denormalises the picklist's
    // display name into {@link #dose} / {@link #route} / {@link #frequency}
    // so existing read-paths keep working without joining masterdata.
    @Setter @Column(name = "dosage_uid",    length = 26) private String dosageUid;
    @Setter @Column(name = "route_uid",     length = 26) private String routeUid;
    @Setter @Column(name = "route",         length = 80) private String route;
    @Setter @Column(name = "frequency_uid", length = 26) private String frequencyUid;

    // ----- multi-pharmacy dispense (Phase 37) ------------------------------
    // Both populated at dispense time. Equal in the common case; differ when
    // the prescription is filled at pharmacy A but stock is pulled from
    // pharmacy B without a formal transfer document.
    @Setter @Column(name = "issue_pharmacy_uid", length = 26) private String issuePharmacyUid;
    @Setter @Column(name = "sales_pharmacy_uid", length = 26) private String salesPharmacyUid;

    @Column(name = "requested_at", nullable = false) private Instant requestedAt;
    @Setter @Column(name = "accepted_at")  private Instant acceptedAt;
    @Setter @Column(name = "held_at")      private Instant heldAt;
    @Setter @Column(name = "verified_at")  private Instant verifiedAt;
    @Setter @Column(name = "approved_at")  private Instant approvedAt;
    @Setter @Column(name = "dispensed_at") private Instant dispensedAt;
    @Setter @Column(name = "rejected_at")  private Instant rejectedAt;
    @Setter @Column(name = "reject_reason",  length = 255) private String rejectReason;
    @Setter @Column(name = "cancel_reason", length = 255) private String cancelReason;

    public Prescription(String prescriptionNo, String consultationUid, String patientUid,
                        String medicineUid, String dose, String frequency,
                        Integer durationDays, Integer quantity, String instructions) {
        this.prescriptionNo = prescriptionNo;
        this.consultationUid = consultationUid;
        this.patientUid = patientUid;
        this.medicineUid = medicineUid;
        this.dose = dose;
        this.frequency = frequency;
        this.durationDays = durationDays;
        this.quantity = quantity;
        this.instructions = instructions;
        this.requestedAt = Instant.now();
    }

    /** Pharmacist picks the Rx off the queue. PENDING → ACCEPTED. */
    public void accept() {
        if (status != PrescriptionStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING prescriptions can be accepted (current: " + status + ")");
        }
        status = PrescriptionStatus.ACCEPTED;
        acceptedAt = Instant.now();
    }

    /** Pause: awaiting payment / stock arrival. ACCEPTED | VERIFIED → HELD. */
    public void hold() {
        if (status != PrescriptionStatus.ACCEPTED && status != PrescriptionStatus.VERIFIED) {
            throw new BusinessRuleException("Cannot hold from " + status);
        }
        status = PrescriptionStatus.HELD;
        heldAt = Instant.now();
    }

    /** Clinical / stock quality check passed. ACCEPTED | HELD → VERIFIED. */
    public void verify() {
        if (status != PrescriptionStatus.ACCEPTED && status != PrescriptionStatus.HELD) {
            throw new BusinessRuleException("Cannot verify from " + status);
        }
        status = PrescriptionStatus.VERIFIED;
        verifiedAt = Instant.now();
    }

    /** Final approval, ready for dispense. VERIFIED → APPROVED. */
    public void approve() {
        if (status != PrescriptionStatus.VERIFIED) {
            throw new BusinessRuleException("Only VERIFIED prescriptions can be approved (current: " + status + ")");
        }
        status = PrescriptionStatus.APPROVED;
        approvedAt = Instant.now();
    }

    /**
     * Mark as dispensed once stock has been decremented. APPROVED → SOLD.
     * Called from the pharmacy stock service inside the same transaction
     * as the stock movement.
     */
    public void markSold() {
        if (status != PrescriptionStatus.APPROVED) {
            throw new BusinessRuleException("Only APPROVED prescriptions can be sold (current: " + status + ")");
        }
        status = PrescriptionStatus.SOLD;
        dispensedAt = Instant.now();
    }

    /** Pharmacist refused. Allowed from any pre-SOLD pharmacy state. Terminal. */
    public void reject(String reason) {
        if (status == PrescriptionStatus.SOLD || status == PrescriptionStatus.CANCELLED
                || status == PrescriptionStatus.REJECTED) {
            throw new BusinessRuleException("Cannot reject from " + status);
        }
        status = PrescriptionStatus.REJECTED;
        rejectedAt = Instant.now();
        rejectReason = reason;
    }

    /** Withdrawn by prescriber before pharmacy has worked it. PENDING → CANCELLED. */
    public void cancel(String reason) {
        if (status == PrescriptionStatus.SOLD) {
            throw new BusinessRuleException("Sold prescriptions cannot be cancelled");
        }
        if (status == PrescriptionStatus.CANCELLED) return;
        status = PrescriptionStatus.CANCELLED;
        cancelReason = reason;
    }
}
