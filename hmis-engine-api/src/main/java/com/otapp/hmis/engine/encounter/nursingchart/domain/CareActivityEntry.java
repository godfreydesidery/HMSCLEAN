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
 * One row on the patient's care-activity chart (legacy {@code PatientNursingChart}
 * feeding / changingPosition / bedBathing / randomBloodSugar / fullBloodSugar):
 * the per-shift log of nursing tasks performed plus any bedside blood-sugar
 * readings. Immutable once recorded — a correction means a new entry.
 */
@Entity
@Table(name = "care_activity_entry",
       indexes = {
               @Index(name = "idx_care_activity_admission", columnList = "admission_uid, recorded_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareActivityEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_uid", nullable = false, length = 26)
    private String admissionUid;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by_username", nullable = false, length = 64)
    private String recordedByUsername;

    @Column(name = "feeding_done", nullable = false)     private boolean feedingDone;
    @Column(name = "position_changed", nullable = false) private boolean positionChanged;
    @Column(name = "bed_bath_done", nullable = false)    private boolean bedBathDone;

    /** Random (casual) bedside blood-sugar reading, mmol/L. */
    @Column(name = "random_blood_sugar_mmol", precision = 5, scale = 2) private BigDecimal randomBloodSugarMmol;

    /** Fasting bedside blood-sugar reading, mmol/L (legacy "full" blood sugar). */
    @Column(name = "fasting_blood_sugar_mmol", precision = 5, scale = 2) private BigDecimal fastingBloodSugarMmol;

    @Column(name = "notes", length = 500) private String notes;

    @SuppressWarnings("java:S107") // a flat per-shift checklist; the fields are the data
    public CareActivityEntry(String admissionUid, String recordedByUsername,
                             boolean feedingDone, boolean positionChanged, boolean bedBathDone,
                             BigDecimal randomBloodSugarMmol, BigDecimal fastingBloodSugarMmol, String notes) {
        boolean anyActivity = feedingDone || positionChanged || bedBathDone;
        boolean anyReading = randomBloodSugarMmol != null || fastingBloodSugarMmol != null;
        boolean anyNote = notes != null && !notes.isBlank();
        if (!anyActivity && !anyReading && !anyNote) {
            throw new BusinessRuleException(
                    "Care-activity entry must record at least one activity, a blood-sugar reading, or a note");
        }
        this.admissionUid = admissionUid;
        this.recordedByUsername = recordedByUsername;
        this.recordedAt = Instant.now();
        this.feedingDone = feedingDone;
        this.positionChanged = positionChanged;
        this.bedBathDone = bedBathDone;
        this.randomBloodSugarMmol = randomBloodSugarMmol;
        this.fastingBloodSugarMmol = fastingBloodSugarMmol;
        this.notes = notes;
    }
}
