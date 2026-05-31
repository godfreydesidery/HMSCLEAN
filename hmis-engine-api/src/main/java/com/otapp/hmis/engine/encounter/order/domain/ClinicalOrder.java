package com.otapp.hmis.engine.encounter.order.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A request for a clinical service (lab test, radiology, procedure) raised
 * during a consultation. The {@link #kind} + {@link #serviceUid} pair points
 * polymorphically at one of the masterdata catalogues.
 */
@Entity
@Table(name = "clinical_order",
       uniqueConstraints = @UniqueConstraint(name = "uk_clinical_order_no", columnNames = "order_no"),
       indexes = {
               @Index(name = "idx_clinical_order_consultation", columnList = "consultation_uid"),
               @Index(name = "idx_clinical_order_status",       columnList = "status"),
               @Index(name = "idx_clinical_order_kind",         columnList = "kind"),
               @Index(name = "idx_clinical_order_service",      columnList = "kind, service_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 32)
    private String orderNo;

    /** Null for OUTSIDER (walk-in) orders raised directly against the patient. */
    @Column(name = "consultation_uid", length = 26) private String consultationUid;
    @Column(name = "patient_uid",      nullable = false, length = 26) private String patientUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ClinicalOrderKind kind;

    /** Public uid of the lab test / radiology / procedure type. */
    @Column(name = "service_uid", nullable = false, length = 26)
    private String serviceUid;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ClinicalOrderStatus status = ClinicalOrderStatus.REQUESTED;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderUrgency urgency = OrderUrgency.NORMAL;

    /**
     * Denormalised payment flag set by the billing settlement dispatcher when
     * the invoice carrying this order's line is paid. Surfaced on the role
     * worklists so a technician can scope to settled work (the encounter module
     * never reads billing).
     */
    @Column(name = "settled", nullable = false) private boolean settled = false;
    @Setter @Column(name = "settled_at") private Instant settledAt;

    @Column(name = "requested_at", nullable = false) private Instant requestedAt;
    @Setter @Column(name = "accepted_at") private Instant acceptedAt;
    @Setter @Column(name = "approved_at") private Instant approvedAt;
    @Setter @Column(name = "approved_by_username", length = 64) private String approvedByUsername;
    @Setter @Column(name = "completed_at") private Instant completedAt;

    @Setter @Column(name = "instructions", length = 1000) private String instructions;
    @Setter @Column(name = "result",       length = 4000) private String result;
    @Setter @Column(name = "cancel_reason", length = 255)  private String cancelReason;

    // ----- procedure-only scheduling fields (PROCESS.md §7, Phase 24) ------
    /** Theatre booked for this procedure. Always null for LAB_TEST / RADIOLOGY. */
    @Setter @Column(name = "theatre_uid",   length = 26) private String theatreUid;
    @Setter @Column(name = "scheduled_at")               private Instant scheduledAt;
    @Setter @Column(name = "scheduled_by_username", length = 64) private String scheduledByUsername;

    // ----- reject / hold audit (lab + radiology bounce-back) ----------------
    // Encapsulated — only the reject()/hold()/accept() transitions touch these.
    @Column(name = "rejected_at")                    private Instant rejectedAt;
    @Column(name = "rejected_by_username", length = 64) private String rejectedByUsername;
    @Column(name = "reject_reason", length = 255)    private String rejectReason;
    @Column(name = "held_at")                        private Instant heldAt;
    @Column(name = "held_by_username", length = 64)  private String heldByUsername;

    public ClinicalOrder(String orderNo, String consultationUid, String patientUid,
                         ClinicalOrderKind kind, String serviceUid,
                         OrderUrgency urgency, String instructions) {
        this.orderNo = orderNo;
        this.consultationUid = consultationUid;
        this.patientUid = patientUid;
        this.kind = kind;
        this.serviceUid = serviceUid;
        this.urgency = urgency == null ? OrderUrgency.NORMAL : urgency;
        this.instructions = instructions;
        this.requestedAt = Instant.now();
    }

    /**
     * Books a theatre + time slot for a procedure order. Idempotent —
     * re-scheduling a still-open procedure simply overwrites the booking.
     */
    public void schedule(String theatreUid, Instant scheduledAt, String scheduledByUsername) {
        if (kind != ClinicalOrderKind.PROCEDURE) {
            throw new BusinessRuleException("Only procedure orders can be scheduled (kind: " + kind + ")");
        }
        if (status == ClinicalOrderStatus.COMPLETED || status == ClinicalOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot schedule a " + status + " order");
        }
        this.theatreUid = theatreUid;
        this.scheduledAt = scheduledAt;
        this.scheduledByUsername = scheduledByUsername;
    }

    /** Idempotent — flags this order's charge as settled. */
    public void markSettled() {
        if (!settled) {
            settled = true;
            settledAt = Instant.now();
        }
    }

    /**
     * Lab / radiology acceptance gate: REQUESTED → ACCEPTED (specimen collected
     * or study scheduled and accepted). Procedures use {@link #approve} instead.
     */
    public void accept() {
        if (kind == ClinicalOrderKind.PROCEDURE) {
            throw new BusinessRuleException("Procedures are signed off via approve(), not accept()");
        }
        // Legacy re-accept loop: a REJECTED order is recovered by accepting it
        // again (a held order was bounced to REQUESTED, so it's covered too).
        if (status != ClinicalOrderStatus.REQUESTED && status != ClinicalOrderStatus.REJECTED) {
            throw new BusinessRuleException(
                    "Only REQUESTED or REJECTED orders can be accepted (current: " + status + ")");
        }
        // Re-accepting clears the rejection audit, as the legacy did.
        rejectedAt = null;
        rejectedByUsername = null;
        rejectReason = null;
        status = ClinicalOrderStatus.ACCEPTED;
        acceptedAt = Instant.now();
    }

    /**
     * Lab / radiology rejection: a specimen/study is bounced back with a reason.
     * REQUESTED or ACCEPTED → REJECTED, clearing the accept stamp. Recoverable —
     * {@link #accept()} re-accepts it. Procedures have no reject path in legacy.
     */
    public void reject(String reason, String username) {
        if (kind == ClinicalOrderKind.PROCEDURE) {
            throw new BusinessRuleException("Procedures cannot be rejected (use cancel)");
        }
        if (status != ClinicalOrderStatus.REQUESTED && status != ClinicalOrderStatus.ACCEPTED) {
            throw new BusinessRuleException(
                    "Only REQUESTED or ACCEPTED orders can be rejected (current: " + status + ")");
        }
        status = ClinicalOrderStatus.REJECTED;
        rejectedAt = Instant.now();
        rejectedByUsername = username;
        rejectReason = reason;
        acceptedAt = null;
    }

    /**
     * Lab / radiology hold: an ACCEPTED order is paused and returned to the
     * pending queue (legacy bounces it to PENDING and stamps who held it — there
     * is no distinct HELD state). Resume by accepting it again.
     */
    public void hold(String username) {
        if (kind == ClinicalOrderKind.PROCEDURE) {
            throw new BusinessRuleException("Procedures cannot be held");
        }
        if (status != ClinicalOrderStatus.ACCEPTED) {
            throw new BusinessRuleException("Only ACCEPTED orders can be held (current: " + status + ")");
        }
        status = ClinicalOrderStatus.REQUESTED;
        heldAt = Instant.now();
        heldByUsername = username;
        acceptedAt = null;
    }

    /**
     * Procedure approval gate: REQUESTED → APPROVED (surgeon / anaesthetist
     * sign-off). Only valid for PROCEDURE orders.
     */
    public void approve(String approverUsername) {
        if (kind != ClinicalOrderKind.PROCEDURE) {
            throw new BusinessRuleException("Only procedures require approval (kind: " + kind + ")");
        }
        if (status != ClinicalOrderStatus.REQUESTED) {
            throw new BusinessRuleException("Only REQUESTED procedures can be approved (current: " + status + ")");
        }
        status = ClinicalOrderStatus.APPROVED;
        approvedAt = Instant.now();
        approvedByUsername = approverUsername;
    }

    public void markInProgress() {
        boolean gatePassed = (kind == ClinicalOrderKind.PROCEDURE)
                ? status == ClinicalOrderStatus.APPROVED
                : status == ClinicalOrderStatus.ACCEPTED;
        if (!gatePassed) {
            String gate = kind == ClinicalOrderKind.PROCEDURE ? "APPROVED" : "ACCEPTED";
            throw new BusinessRuleException(
                    "Order must be " + gate + " before work begins (current: " + status + ")");
        }
        status = ClinicalOrderStatus.IN_PROGRESS;
    }

    public void complete(String result) {
        if (status != ClinicalOrderStatus.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "Only IN_PROGRESS orders can be completed (current: " + status + ")");
        }
        // Pay-before-service gate (M13): a consultation-bound order's bill must be
        // settled before its result is released. Non-CASH / zero-price orders are
        // settled at billing; outsider-direct orders (no consultation) are exempt.
        if (consultationUid != null && !settled) {
            throw new BusinessRuleException(
                    "The order's bill must be settled before it can be completed (collect payment first)");
        }
        status = ClinicalOrderStatus.COMPLETED;
        this.result = result;
        completedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (status == ClinicalOrderStatus.COMPLETED) {
            throw new BusinessRuleException("Completed orders cannot be cancelled");
        }
        status = ClinicalOrderStatus.CANCELLED;
        cancelReason = reason;
    }
}
