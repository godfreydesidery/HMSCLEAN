package com.otapp.hmis.engine.encounter.vitals.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A single vital-signs reading taken during a consultation. Multiple readings
 * may be recorded against the same consultation (e.g. on admission, after
 * triage, on review).
 */
@Entity
@Table(name = "patient_vitals", indexes = {
        @Index(name = "idx_patient_vitals_consultation", columnList = "consultation_uid"),
        @Index(name = "idx_patient_vitals_patient",      columnList = "patient_uid")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientVitals extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_uid", nullable = false, length = 26) private String consultationUid;
    @Column(name = "patient_uid",      nullable = false, length = 26) private String patientUid;

    @Column(name = "taken_at", nullable = false)
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

    public PatientVitals(String consultationUid, String patientUid, Instant takenAt) {
        this.consultationUid = consultationUid;
        this.patientUid = patientUid;
        this.takenAt = takenAt;
    }
}
