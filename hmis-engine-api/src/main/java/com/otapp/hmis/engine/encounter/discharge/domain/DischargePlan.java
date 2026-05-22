package com.otapp.hmis.engine.encounter.discharge.domain;

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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Structured closing document for an inpatient admission (PROCESS.md
 * §3.3). One per admission. Authored by the discharging clinician with
 * structured clinical fields (history, investigation, management, op
 * note, ICU note, recommendations) and then approved by a ward
 * administrator — the APPROVED transition is what closes the underlying
 * admission.
 *
 * <p>The legacy Zana-HMIS uses three sibling documents (discharge note,
 * deceased note, referral plan) that share most fields and differ only
 * by closure-path semantics. This entity collapses them into one
 * aggregate discriminated by {@link DischargePlanKind} — kind-specific
 * fields are nullable and validated by the service.
 */
@Entity
@Table(name = "discharge_plan",
       uniqueConstraints = @UniqueConstraint(name = "uk_discharge_plan_admission",
                                             columnNames = "admission_uid"),
       indexes = {
               @Index(name = "idx_discharge_plan_status",   columnList = "status"),
               @Index(name = "idx_discharge_plan_kind",     columnList = "kind"),
               @Index(name = "idx_discharge_plan_authored", columnList = "authored_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DischargePlan extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_uid", nullable = false, length = 26) private String admissionUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DischargePlanKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DischargePlanStatus status = DischargePlanStatus.PENDING;

    // ----- structured clinical narrative (all kinds) -----------------------

    @Setter @Column(name = "history",          length = 4000) private String history;
    @Setter @Column(name = "investigation",    length = 4000) private String investigation;
    @Setter @Column(name = "management",       length = 4000) private String management;
    @Setter @Column(name = "operation_note",   length = 4000) private String operationNote;
    @Setter @Column(name = "icu_note",         length = 4000) private String icuNote;
    @Setter @Column(name = "recommendations",  length = 4000) private String recommendations;

    // ----- kind-specific fields --------------------------------------------

    /** REFERRAL only — receiving facility name. */
    @Setter @Column(name = "referral_facility", length = 200) private String referralFacility;
    /** REFERRAL only — why the patient is being moved. */
    @Setter @Column(name = "referral_reason",   length = 1000) private String referralReason;

    /** DECEASED only — recorded time of death. */
    @Setter @Column(name = "time_of_death") private Instant timeOfDeath;
    /** DECEASED only — cause of death. */
    @Setter @Column(name = "cause_of_death", length = 500) private String causeOfDeath;

    // ----- workflow gates --------------------------------------------------

    @Column(name = "authored_by_username", nullable = false, length = 64)
    private String authoredByUsername;
    @Column(name = "authored_at",          nullable = false)
    private Instant authoredAt;

    @Setter @Column(name = "approved_by_username", length = 64) private String approvedByUsername;
    @Setter @Column(name = "approved_at")                       private Instant approvedAt;

    @Setter @Column(name = "cancelled_by_username", length = 64) private String cancelledByUsername;
    @Setter @Column(name = "cancelled_at")                       private Instant cancelledAt;
    @Setter @Column(name = "cancel_reason", length = 255)        private String cancelReason;

    public DischargePlan(String admissionUid, DischargePlanKind kind, String authoredByUsername) {
        this.admissionUid = admissionUid;
        this.kind = kind;
        this.authoredByUsername = authoredByUsername;
        this.authoredAt = Instant.now();
    }

    public boolean isEditable() {
        return status == DischargePlanStatus.PENDING;
    }

    public boolean isApproved() {
        return status == DischargePlanStatus.APPROVED;
    }

    public void approve(String approverUsername) {
        if (status != DischargePlanStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only PENDING plans can be approved (current: " + status + ")");
        }
        if (authoredByUsername.equals(approverUsername)) {
            throw new BusinessRuleException(
                    "A plan cannot be approved by its author — a separate approver is required");
        }
        status = DischargePlanStatus.APPROVED;
        approvedByUsername = approverUsername;
        approvedAt = Instant.now();
    }

    public void cancel(String cancellerUsername, String reason) {
        if (status == DischargePlanStatus.CANCELLED) return;
        if (status == DischargePlanStatus.APPROVED) {
            throw new BusinessRuleException("APPROVED plans cannot be cancelled");
        }
        status = DischargePlanStatus.CANCELLED;
        cancelledByUsername = cancellerUsername;
        cancelledAt = Instant.now();
        cancelReason = reason;
    }

    /** Enforced by the service before save; called from the constructor flow. */
    public void requireEditable() {
        if (!isEditable()) {
            throw new BusinessRuleException(
                    "Plan can only be edited while PENDING (current: " + status + ")");
        }
    }
}
