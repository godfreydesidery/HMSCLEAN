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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row on the patient's fluid-balance chart (legacy {@code PatientNursingChart}
 * fluidIntake / urineOutput / drainageOutput): millilitres in and out at a point
 * in the admission. Multiple entries build the running input/output trend the
 * ward (especially ICU/HDU) needs. Immutable once recorded — a correction means a
 * new entry, matching {@link AdmissionVitalsEntry} / {@link DressingChartEntry}.
 */
@Entity
@Table(name = "fluid_balance_entry",
       indexes = {
               @Index(name = "idx_fluid_balance_admission", columnList = "admission_uid, recorded_at"),
               @Index(name = "idx_fluid_balance_recorded",  columnList = "recorded_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FluidBalanceEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_uid", nullable = false, length = 26)
    private String admissionUid;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by_username", nullable = false, length = 64)
    private String recordedByUsername;

    /** Total fluid taken in (oral + IV) this entry, in millilitres. */
    @Column(name = "intake_ml") private Integer intakeMl;

    /** Urine passed this entry, in millilitres. */
    @Column(name = "urine_output_ml") private Integer urineOutputMl;

    /** Other output — drain / NG aspirate / vomit — this entry, in millilitres. */
    @Column(name = "drainage_output_ml") private Integer drainageOutputMl;

    @Column(name = "notes", length = 500) private String notes;

    public FluidBalanceEntry(String admissionUid, String recordedByUsername,
                             Integer intakeMl, Integer urineOutputMl, Integer drainageOutputMl, String notes) {
        if (intakeMl == null && urineOutputMl == null && drainageOutputMl == null) {
            throw new BusinessRuleException("Fluid-balance entry must record at least one of intake / urine / drainage");
        }
        requireNonNegative("intakeMl", intakeMl);
        requireNonNegative("urineOutputMl", urineOutputMl);
        requireNonNegative("drainageOutputMl", drainageOutputMl);
        this.admissionUid = admissionUid;
        this.recordedByUsername = recordedByUsername;
        this.recordedAt = Instant.now();
        this.intakeMl = intakeMl;
        this.urineOutputMl = urineOutputMl;
        this.drainageOutputMl = drainageOutputMl;
        this.notes = notes;
    }

    /** Total output (urine + drainage) for this entry. */
    public int outputMl() {
        return zero(urineOutputMl) + zero(drainageOutputMl);
    }

    /** Net balance for this entry: intake − total output (can be negative). */
    public int netMl() {
        return zero(intakeMl) - outputMl();
    }

    private static int zero(Integer v) {
        return v == null ? 0 : v;
    }

    private static void requireNonNegative(String field, Integer value) {
        if (value != null && value < 0) {
            throw new BusinessRuleException(field + " must be non-negative");
        }
    }
}
