package com.otapp.hmis.engine.encounter.nursingchart.domain;

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
 * One entry on the patient's dressing chart (PROCESS.md §4): records a
 * wound assessment and the dressing applied at a point in time.
 * Multiple entries per admission build the wound-care trail. Immutable
 * once recorded.
 */
@Entity
@Table(name = "dressing_chart_entry",
       indexes = {
               @Index(name = "idx_dressing_admission", columnList = "admission_uid, recorded_at"),
               @Index(name = "idx_dressing_status",    columnList = "wound_status")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DressingChartEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_uid", nullable = false, length = 26)
    private String admissionUid;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by_username", nullable = false, length = 64)
    private String recordedByUsername;

    /** Anatomical site, e.g. "left lower leg, lateral malleolus". */
    @Column(name = "wound_location", nullable = false, length = 160) private String woundLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "wound_status", nullable = false, length = 16)
    private WoundStatus woundStatus;

    /** What was applied, e.g. "saline + non-adherent dressing + crepe". */
    @Column(name = "dressing_applied", nullable = false, length = 500) private String dressingApplied;

    @Column(length = 1000) private String notes;

    public DressingChartEntry(String admissionUid, String recordedByUsername,
                              String woundLocation, WoundStatus woundStatus,
                              String dressingApplied, String notes) {
        if (woundLocation == null || woundLocation.isBlank()) {
            throw new BusinessRuleException("woundLocation is required");
        }
        if (woundStatus == null) {
            throw new BusinessRuleException("woundStatus is required");
        }
        if (dressingApplied == null || dressingApplied.isBlank()) {
            throw new BusinessRuleException("dressingApplied is required");
        }
        this.admissionUid = admissionUid;
        this.recordedByUsername = recordedByUsername;
        this.recordedAt = Instant.now();
        this.woundLocation = woundLocation;
        this.woundStatus = woundStatus;
        this.dressingApplied = dressingApplied;
        this.notes = notes;
    }
}
