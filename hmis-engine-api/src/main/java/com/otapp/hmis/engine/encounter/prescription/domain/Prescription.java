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

    @Column(name = "consultation_uid", nullable = false, length = 26) private String consultationUid;
    @Column(name = "patient_uid",      nullable = false, length = 26) private String patientUid;
    @Column(name = "medicine_uid",     nullable = false, length = 26) private String medicineUid;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PrescriptionStatus status = PrescriptionStatus.REQUESTED;

    @Setter @Column(name = "dose",          nullable = false, length = 80) private String dose;
    @Setter @Column(name = "frequency",     nullable = false, length = 80) private String frequency;
    @Setter @Column(name = "duration_days") private Integer durationDays;
    @Setter @Column(name = "quantity")      private Integer quantity;
    @Setter @Column(name = "instructions",  length = 500)                  private String instructions;

    @Column(name = "requested_at", nullable = false) private Instant requestedAt;
    @Setter @Column(name = "dispensed_at") private Instant dispensedAt;
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

    public void dispense() {
        if (status != PrescriptionStatus.REQUESTED) {
            throw new BusinessRuleException("Only REQUESTED prescriptions can be dispensed (current: " + status + ")");
        }
        status = PrescriptionStatus.DISPENSED;
        dispensedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (status == PrescriptionStatus.DISPENSED) {
            throw new BusinessRuleException("Dispensed prescriptions cannot be cancelled");
        }
        status = PrescriptionStatus.CANCELLED;
        cancelReason = reason;
    }
}
