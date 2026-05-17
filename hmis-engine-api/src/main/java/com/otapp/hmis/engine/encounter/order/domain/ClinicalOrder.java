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

    @Column(name = "consultation_uid", nullable = false, length = 26) private String consultationUid;
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

    @Column(name = "requested_at", nullable = false) private Instant requestedAt;
    @Setter @Column(name = "completed_at") private Instant completedAt;

    @Setter @Column(name = "instructions", length = 1000) private String instructions;
    @Setter @Column(name = "result",       length = 4000) private String result;
    @Setter @Column(name = "cancel_reason", length = 255)  private String cancelReason;

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

    public void markInProgress() {
        if (status != ClinicalOrderStatus.REQUESTED) {
            throw new BusinessRuleException("Only REQUESTED orders can be started (current: " + status + ")");
        }
        status = ClinicalOrderStatus.IN_PROGRESS;
    }

    public void complete(String result) {
        if (status != ClinicalOrderStatus.REQUESTED && status != ClinicalOrderStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Order cannot be completed from " + status);
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
