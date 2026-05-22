package com.otapp.hmis.engine.procurement.receipt.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One stock-arrival event against a purchase order. May cover one or
 * many lines (full or partial quantities). Per Phase 23a the GRN now
 * carries its own PENDING → VERIFIED → APPROVED workflow: stock is only
 * touched when APPROVED, and the PO line's recordReceipt fires at the
 * same time.
 */
@Entity
@Table(name = "goods_receipt",
       uniqueConstraints = @UniqueConstraint(name = "uk_goods_receipt_no", columnNames = "receipt_no"),
       indexes = {
               @Index(name = "idx_goods_receipt_order",       columnList = "order_uid"),
               @Index(name = "idx_goods_receipt_store",       columnList = "store_uid"),
               @Index(name = "idx_goods_receipt_received_at", columnList = "received_at"),
               @Index(name = "idx_goods_receipt_status",      columnList = "status")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsReceipt extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_no", nullable = false, length = 32)
    private String receiptNo;

    @Column(name = "order_uid", nullable = false, length = 26) private String orderUid;
    @Column(name = "store_uid", nullable = false, length = 26) private String storeUid;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Setter @Column(name = "received_by_username", length = 64) private String receivedByUsername;
    @Setter @Column(name = "delivery_note", length = 120)       private String deliveryNote;
    @Setter @Column(length = 500)                                private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GoodsReceiptStatus status = GoodsReceiptStatus.PENDING;

    @Setter @Column(name = "verified_at")                 private Instant verifiedAt;
    @Setter @Column(name = "verified_by_username", length = 64) private String verifiedByUsername;
    @Setter @Column(name = "approved_at")                 private Instant approvedAt;
    @Setter @Column(name = "approved_by_username", length = 64) private String approvedByUsername;
    @Setter @Column(name = "rejected_at")                 private Instant rejectedAt;
    @Setter @Column(name = "rejected_by_username", length = 64) private String rejectedByUsername;
    @Setter @Column(name = "reject_reason",  length = 255) private String rejectReason;

    public GoodsReceipt(String receiptNo, String orderUid, String storeUid,
                        String receivedByUsername, String deliveryNote, String notes) {
        this.receiptNo = receiptNo;
        this.orderUid = orderUid;
        this.storeUid = storeUid;
        this.receivedByUsername = receivedByUsername;
        this.deliveryNote = deliveryNote;
        this.notes = notes;
        this.receivedAt = Instant.now();
    }

    public void verify(String verifierUsername) {
        if (status != GoodsReceiptStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only PENDING GRNs can be verified (current: " + status + ")");
        }
        status = GoodsReceiptStatus.VERIFIED;
        verifiedAt = Instant.now();
        verifiedByUsername = verifierUsername;
    }

    public void approve(String approverUsername) {
        if (status != GoodsReceiptStatus.VERIFIED) {
            throw new BusinessRuleException(
                    "Only VERIFIED GRNs can be approved (current: " + status + ")");
        }
        status = GoodsReceiptStatus.APPROVED;
        approvedAt = Instant.now();
        approvedByUsername = approverUsername;
    }

    public void reject(String rejecterUsername, String reason) {
        if (status == GoodsReceiptStatus.APPROVED || status == GoodsReceiptStatus.REJECTED) {
            throw new BusinessRuleException(
                    "Cannot reject a " + status + " GRN");
        }
        status = GoodsReceiptStatus.REJECTED;
        rejectedAt = Instant.now();
        rejectedByUsername = rejecterUsername;
        rejectReason = reason;
    }

    public boolean isApproved() {
        return status == GoodsReceiptStatus.APPROVED;
    }
}
