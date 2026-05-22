package com.otapp.hmis.engine.procurement.order.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_order",
       uniqueConstraints = @UniqueConstraint(name = "uk_purchase_order_no", columnNames = "order_no"),
       indexes = {
               @Index(name = "idx_purchase_order_supplier", columnList = "supplier_uid"),
               @Index(name = "idx_purchase_order_store",    columnList = "store_uid"),
               @Index(name = "idx_purchase_order_status",   columnList = "status")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 32)
    private String orderNo;

    @Column(name = "supplier_uid", nullable = false, length = 26) private String supplierUid;

    /** Target store where received stock will land. */
    @Column(name = "store_uid", nullable = false, length = 26) private String storeUid;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Setter @Column(name = "expected_delivery_date") private LocalDate expectedDeliveryDate;

    @Setter @Column(length = 500) private String notes;

    @Setter @Column(name = "verified_at")  private Instant verifiedAt;
    @Setter @Column(name = "approved_at")  private Instant approvedAt;
    @Setter @Column(name = "ordered_at")   private Instant orderedAt;
    @Setter @Column(name = "received_at")  private Instant receivedAt;
    @Setter @Column(name = "rejected_at")  private Instant rejectedAt;
    @Setter @Column(name = "reject_reason", length = 255) private String rejectReason;
    @Setter @Column(name = "cancelled_at") private Instant cancelledAt;
    @Setter @Column(name = "cancel_reason", length = 255) private String cancelReason;

    public PurchaseOrder(String orderNo, String supplierUid, String storeUid,
                         LocalDate expectedDeliveryDate, String notes) {
        this.orderNo = orderNo;
        this.supplierUid = supplierUid;
        this.storeUid = storeUid;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.notes = notes;
    }

    public void verify() {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT purchase orders can be verified (current: " + status + ")");
        }
        status = PurchaseOrderStatus.VERIFIED;
        verifiedAt = Instant.now();
    }

    public void approve() {
        if (status != PurchaseOrderStatus.VERIFIED) {
            throw new BusinessRuleException("Only VERIFIED purchase orders can be approved (current: " + status + ")");
        }
        status = PurchaseOrderStatus.APPROVED;
        approvedAt = Instant.now();
    }

    public void markOrdered() {
        if (status != PurchaseOrderStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Only APPROVED purchase orders can be sent to the supplier (current: " + status + ")");
        }
        status = PurchaseOrderStatus.ORDERED;
        orderedAt = Instant.now();
    }

    public void reject(String reason) {
        if (status != PurchaseOrderStatus.DRAFT
                && status != PurchaseOrderStatus.VERIFIED
                && status != PurchaseOrderStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Only pre-submission orders can be rejected (current: " + status + ")");
        }
        status = PurchaseOrderStatus.REJECTED;
        rejectedAt = Instant.now();
        rejectReason = reason;
    }

    /**
     * Status transition driven by goods receipts on lines. Called after a
     * receipt is recorded against any line. {@code allLinesFullyReceived}
     * is computed by the service.
     */
    public void onLineReceipt(boolean allLinesFullyReceived) {
        if (status != PurchaseOrderStatus.ORDERED && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BusinessRuleException("Cannot receive against a " + status + " order");
        }
        if (allLinesFullyReceived) {
            status = PurchaseOrderStatus.RECEIVED;
            receivedAt = Instant.now();
        } else {
            status = PurchaseOrderStatus.PARTIALLY_RECEIVED;
        }
    }

    public void cancel(String reason) {
        if (status == PurchaseOrderStatus.RECEIVED) {
            throw new BusinessRuleException("Fully received purchase orders cannot be cancelled");
        }
        status = PurchaseOrderStatus.CANCELLED;
        cancelledAt = Instant.now();
        cancelReason = reason;
    }

    public boolean isMutable() {
        return status == PurchaseOrderStatus.DRAFT;
    }
}
