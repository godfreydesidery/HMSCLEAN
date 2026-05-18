package com.otapp.hmis.engine.encounter.nursingchart.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
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

/**
 * One row on the patient observation chart (PROCESS.md §4) — vitals
 * recorded at a specific point during the admission. Multiple entries
 * per admission build the per-shift / per-round trend the ward needs.
 *
 * <p>Immutable once recorded. A typo correction means recording a new
 * entry, not editing an old one — same convention as
 * {@link com.otapp.hmis.engine.encounter.progressnote.domain.ProgressNote}.
 */
@Entity
@Table(name = "admission_vitals_entry",
       indexes = {
               @Index(name = "idx_admission_vitals_admission", columnList = "admission_uid, recorded_at"),
               @Index(name = "idx_admission_vitals_recorded",  columnList = "recorded_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdmissionVitalsEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_uid", nullable = false, length = 26)
    private String admissionUid;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by_username", nullable = false, length = 64)
    private String recordedByUsername;

    /** Body temperature in °C. */
    @Column(name = "temperature_c", precision = 4, scale = 1) private BigDecimal temperatureC;

    /** Beats per minute. */
    @Column(name = "pulse_bpm")           private Integer pulseBpm;
    @Column(name = "respirations_bpm")    private Integer respirationsBpm;

    @Column(name = "systolic_bp")  private Integer systolicBp;
    @Column(name = "diastolic_bp") private Integer diastolicBp;

    /** Peripheral oxygen saturation, percentage 0–100. */
    @Column(name = "spo2_percent") private Integer spo2Percent;

    @Column(name = "blood_glucose_mmol", precision = 5, scale = 2) private BigDecimal bloodGlucoseMmol;

    /** Numeric pain rating 0–10. */
    @Column(name = "pain_score") private Integer painScore;

    @Column(name = "notes", length = 500) private String notes;

    public AdmissionVitalsEntry(String admissionUid, String recordedByUsername,
                                BigDecimal temperatureC, Integer pulseBpm, Integer respirationsBpm,
                                Integer systolicBp, Integer diastolicBp, Integer spo2Percent,
                                BigDecimal bloodGlucoseMmol, Integer painScore, String notes) {
        if (allNull(temperatureC, pulseBpm, respirationsBpm, systolicBp, diastolicBp,
                spo2Percent, bloodGlucoseMmol, painScore)) {
            throw new BusinessRuleException("Vitals entry must record at least one measurement");
        }
        if (painScore != null && (painScore < 0 || painScore > 10)) {
            throw new BusinessRuleException("painScore must be in [0, 10]");
        }
        if (spo2Percent != null && (spo2Percent < 0 || spo2Percent > 100)) {
            throw new BusinessRuleException("spo2Percent must be in [0, 100]");
        }
        this.admissionUid = admissionUid;
        this.recordedByUsername = recordedByUsername;
        this.recordedAt = Instant.now();
        this.temperatureC = temperatureC;
        this.pulseBpm = pulseBpm;
        this.respirationsBpm = respirationsBpm;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.spo2Percent = spo2Percent;
        this.bloodGlucoseMmol = bloodGlucoseMmol;
        this.painScore = painScore;
        this.notes = notes;
    }

    private static boolean allNull(Object... values) {
        for (Object v : values) {
            if (v != null) return false;
        }
        return true;
    }
}
