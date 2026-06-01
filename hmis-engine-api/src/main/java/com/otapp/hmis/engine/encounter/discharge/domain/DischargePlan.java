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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Structured closing document — the unified "closure plan". A plan closes
 * EITHER an inpatient admission OR an outpatient consultation (exactly one of
 * {@code admissionUid} / {@code consultationUid} is set, recorded by
 * {@link #subjectType}). Authored by the clinician with structured clinical
 * fields (history, investigation, management, op note, ICU note,
 * recommendations) and then approved by a second user — the APPROVED transition
 * is what closes the underlying admission / consultation.
 *
 * <p>The legacy Zana-HMIS uses sibling documents (discharge note, deceased
 * note, referral plan), each of which itself keyed off either an admission or a
 * consultation FK. This entity collapses them into one aggregate discriminated
 * by {@link DischargePlanKind} (and {@link ClosureSubject}) — kind/subject-
 * specific fields are nullable and validated by the service. A consultation
 * subject only allows DECEASED / REFERRAL kinds.
 *
 * <p>The per-subject 1:1 uniqueness is enforced by partial unique indexes in
 * the Flyway migration (one plan per admission, one per consultation), not by a
 * table-level constraint, because each subject uid is nullable.
 */
@Entity
@Table(name = "discharge_plan",
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

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 16)
    private ClosureSubject subjectType;

    /** Set when {@code subjectType == ADMISSION}; null otherwise. */
    @Column(name = "admission_uid", length = 26) private String admissionUid;

    /** Set when {@code subjectType == CONSULTATION}; null otherwise. */
    @Column(name = "consultation_uid", length = 26) private String consultationUid;

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

    /** REFERRAL only — receiving facility name (denormalised from the master, or free text). */
    @Setter @Column(name = "referral_facility", length = 200) private String referralFacility;
    /** REFERRAL only — optional FK (by uid) to the ExternalMedicalProvider master. */
    @Setter @Column(name = "external_provider_uid", length = 26) private String externalProviderUid;
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

    private DischargePlan(ClosureSubject subjectType, String admissionUid, String consultationUid,
                          DischargePlanKind kind, String authoredByUsername) {
        this.subjectType = subjectType;
        this.admissionUid = admissionUid;
        this.consultationUid = consultationUid;
        this.kind = kind;
        this.authoredByUsername = authoredByUsername;
        this.authoredAt = Instant.now();
    }

    /** A closure plan for an inpatient admission — all three kinds allowed. */
    public static DischargePlan forAdmission(String admissionUid, DischargePlanKind kind, String authoredByUsername) {
        return new DischargePlan(ClosureSubject.ADMISSION, admissionUid, null, kind, authoredByUsername);
    }

    /** A closure plan for an outpatient consultation — DECEASED / REFERRAL only. */
    public static DischargePlan forConsultation(String consultationUid, DischargePlanKind kind, String authoredByUsername) {
        if (kind != DischargePlanKind.DECEASED && kind != DischargePlanKind.REFERRAL) {
            throw new BusinessRuleException(
                    "An outpatient consultation can only be closed as DECEASED or REFERRAL (was: " + kind + ")");
        }
        return new DischargePlan(ClosureSubject.CONSULTATION, null, consultationUid, kind, authoredByUsername);
    }

    public boolean isEditable() {
        return status == DischargePlanStatus.PENDING;
    }

    public boolean isApproved() {
        return status == DischargePlanStatus.APPROVED;
    }

    /**
     * Approves a PENDING plan.
     *
     * @param allowSelfApproval when {@code false} (the default control) the
     *        author may not also approve — a separate approver is required.
     *        Solo-clinician sites pass {@code true} (config-gated) so a single
     *        doctor can close their own plan.
     */
    public void approve(String approverUsername, boolean allowSelfApproval) {
        if (status != DischargePlanStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only PENDING plans can be approved (current: " + status + ")");
        }
        if (!allowSelfApproval && authoredByUsername.equals(approverUsername)) {
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
