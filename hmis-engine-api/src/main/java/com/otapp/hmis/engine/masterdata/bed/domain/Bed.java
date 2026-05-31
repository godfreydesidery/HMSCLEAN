package com.otapp.hmis.engine.masterdata.bed.domain;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One physical bed in a ward (PROCESS.md §17.13 — replaces the previous
 * "ward.capacity int + free-text admission.bed_label" placeholder with a
 * real bed model). Bed status is updated by {@code AdmissionService} on
 * admit / transfer / discharge so the bed-occupancy report reflects
 * actual physical state.
 *
 * <p>{@link #label} is unique within a ward. The status state machine
 * lives on the entity; ops are validated via {@link #requireFree()}
 * before claiming, etc.
 */
@Entity
@Table(name = "md_bed",
       uniqueConstraints = @UniqueConstraint(name = "uk_md_bed_ward_label",
                                             columnNames = {"ward_uid", "label"}),
       indexes = {
               @Index(name = "idx_md_bed_ward",   columnList = "ward_uid"),
               @Index(name = "idx_md_bed_status", columnList = "status")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bed extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ward_uid", nullable = false, length = 26)
    private String wardUid;

    @Setter @Column(nullable = false, length = 32)  private String label;
    @Setter @Column(length = 500)                    private String notes;
    @Setter @Column(nullable = false)                private boolean active = true;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BedStatus status = BedStatus.FREE;

    /** Set while status is OCCUPIED / RESERVED — uid of the admission holding the bed. */
    @Setter @Column(name = "occupied_by_admission_uid", length = 26)
    private String occupiedByAdmissionUid;

    public Bed(String wardUid, String label, String notes) {
        if (label == null || label.isBlank()) {
            throw new BusinessRuleException("Bed label is required");
        }
        this.wardUid = wardUid;
        this.label = label;
        this.notes = notes;
    }

    public void claim(String admissionUid) {
        requireFree();
        status = BedStatus.OCCUPIED;
        occupiedByAdmissionUid = admissionUid;
    }

    /**
     * Hold the bed for a deposit-pending (AWAITING_DEPOSIT) admission: FREE → RESERVED.
     * The bed is committed to this admission but not yet physically occupied; it
     * occupies on {@link #occupy()} once the deposit is settled, or frees on
     * {@link #release()} if the admission is cancelled.
     */
    public void reserve(String admissionUid) {
        requireFree();
        status = BedStatus.RESERVED;
        occupiedByAdmissionUid = admissionUid;
    }

    /**
     * Take physical possession once the deposit is settled: RESERVED → OCCUPIED.
     * Idempotent — a no-op if the bed is already OCCUPIED (settlement may re-fire);
     * throws if the bed is not currently held (RESERVED).
     */
    public void occupy() {
        if (status == BedStatus.OCCUPIED) {
            return;
        }
        if (status != BedStatus.RESERVED) {
            throw new BusinessRuleException("Bed " + label + " is not reserved (current: " + status + ")");
        }
        status = BedStatus.OCCUPIED;
    }

    public void release() {
        if (status == BedStatus.OUT_OF_SERVICE) {
            // Releasing a bed that was put out of service mid-stay is fine; the
            // physical claim is gone but the bed stays unavailable until
            // markFree() is called.
            occupiedByAdmissionUid = null;
            return;
        }
        status = BedStatus.FREE;
        occupiedByAdmissionUid = null;
    }

    public void markOutOfService(String reason) {
        // Allowed from any state — the caller may need to take a bed offline
        // while occupied (e.g. equipment failure mid-stay).
        status = BedStatus.OUT_OF_SERVICE;
        if (reason != null && !reason.isBlank()) {
            notes = reason;
        }
    }

    public void markFree() {
        status = BedStatus.FREE;
        occupiedByAdmissionUid = null;
    }

    public void requireFree() {
        if (!active) {
            throw new BusinessRuleException("Bed is not active: " + label);
        }
        if (status != BedStatus.FREE) {
            throw new BusinessRuleException("Bed " + label + " is not free (current: " + status + ")");
        }
    }
}
