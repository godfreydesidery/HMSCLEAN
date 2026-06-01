package com.otapp.hmis.engine.encounter.vitals.domain;

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
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single vital-signs reading taken during a consultation, carrying the
 * legacy {@code PatientVital} status lifecycle
 * (EMPTY → PENDING → SUBMITTED → ARCHIVED, see {@link VitalsStatus}).
 *
 * <p>An EMPTY row is materialised when the encounter needs vitals; the nurse
 * fills it (PENDING), submits it (SUBMITTED, locked) and the doctor consumes it
 * into the clinical note (ARCHIVED). Multiple readings may exist against the
 * same consultation (e.g. on triage, on review), but at most one is "open"
 * (EMPTY/PENDING) at a time.
 */
@Entity
@Table(name = "patient_vitals", indexes = {
        @Index(name = "idx_patient_vitals_consultation", columnList = "consultation_uid"),
        @Index(name = "idx_patient_vitals_patient",      columnList = "patient_uid"),
        @Index(name = "idx_patient_vitals_status",       columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientVitals extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_uid", nullable = false, length = 26) private String consultationUid;
    @Column(name = "patient_uid",      nullable = false, length = 26) private String patientUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private VitalsStatus status = VitalsStatus.EMPTY;

    @Column(name = "submitted_at") private Instant submittedAt;
    @Column(name = "archived_at")  private Instant archivedAt;

    @Setter @Column(name = "taken_at", nullable = false)
    private Instant takenAt;

    @Setter @Column(name = "temperature_c",    precision = 4, scale = 1) private BigDecimal temperatureC;
    @Setter @Column(name = "pulse_bpm")           private Integer pulseBpm;
    @Setter @Column(name = "respiration_bpm")     private Integer respirationBpm;
    @Setter @Column(name = "blood_pressure_sys")  private Integer bloodPressureSystolic;
    @Setter @Column(name = "blood_pressure_dia")  private Integer bloodPressureDiastolic;
    @Setter @Column(name = "spo2_percent")        private Integer spo2Percent;
    @Setter @Column(name = "weight_kg", precision = 5, scale = 1) private BigDecimal weightKg;
    @Setter @Column(name = "height_cm", precision = 5, scale = 1) private BigDecimal heightCm;
    @Setter @Column(name = "notes", length = 500) private String notes;

    // ----- clinical-note-safety derived measurements (legacy GeneralExamination) ----
    // bmi (kg/m^2) and bsa (m^2, Mosteller). Stored as NUMERIC measured values
    // (never float); recomputable server-side from weightKg/heightCm. bmiComment
    // preserves any free-text narrative the clinician attaches.
    @Setter @Column(name = "bmi", precision = 4, scale = 1) private BigDecimal bmi;
    @Setter @Column(name = "bsa", precision = 4, scale = 2) private BigDecimal bsa;
    @Setter @Column(name = "bmi_comment", length = 255) private String bmiComment;

    public PatientVitals(String consultationUid, String patientUid, Instant takenAt) {
        this.consultationUid = consultationUid;
        this.patientUid = patientUid;
        this.takenAt = takenAt;
    }

    /** Materialise an EMPTY vitals row — the implicit "needs vitals" request. */
    public static PatientVitals empty(String consultationUid, String patientUid, Instant takenAt) {
        PatientVitals v = new PatientVitals(consultationUid, patientUid, takenAt);
        v.status = VitalsStatus.EMPTY;
        return v;
    }

    /** A still-editable row (the nurse can save / re-save the readings into it). */
    public boolean isOpen() {
        return status == VitalsStatus.EMPTY || status == VitalsStatus.PENDING;
    }

    /**
     * Nurse fill/save: the readings have been entered (or re-entered), so an
     * EMPTY row becomes PENDING. A SUBMITTED/ARCHIVED row is locked. Idempotent
     * for an already-PENDING row (re-save).
     */
    public void markFilled() {
        if (!isOpen()) {
            throw new BusinessRuleException(
                    "Vitals can only be saved while EMPTY or PENDING (current: " + status + ")");
        }
        status = VitalsStatus.PENDING;
    }

    /**
     * Nurse submit: PENDING → SUBMITTED. The set is locked thereafter — a
     * SUBMITTED/ARCHIVED/EMPTY row cannot be submitted.
     */
    public void submit() {
        if (status != VitalsStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only a PENDING vitals set can be submitted (current: " + status + ")");
        }
        status = VitalsStatus.SUBMITTED;
        submittedAt = Instant.now();
    }

    /**
     * Doctor consume: SUBMITTED → ARCHIVED — the captured set has been taken
     * into the clinical exam. Only a SUBMITTED set can be consumed; terminal.
     */
    public void consume() {
        if (status != VitalsStatus.SUBMITTED) {
            throw new BusinessRuleException(
                    "Only a SUBMITTED vitals set can be consumed (current: " + status + ")");
        }
        status = VitalsStatus.ARCHIVED;
        archivedAt = Instant.now();
    }
}
