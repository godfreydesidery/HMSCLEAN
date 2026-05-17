package com.otapp.hmis.engine.procurement.receipt.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One stock-arrival event against a purchase order. May cover one or
 * many lines (full or partial quantities). Each receipt drives a
 * RECEIPT movement on the target pharmacy's stock balance.
 */
@Entity
@Table(name = "goods_receipt",
       uniqueConstraints = @UniqueConstraint(name = "uk_goods_receipt_no", columnNames = "receipt_no"),
       indexes = {
               @Index(name = "idx_goods_receipt_order",    columnList = "order_uid"),
               @Index(name = "idx_goods_receipt_pharmacy", columnList = "pharmacy_uid"),
               @Index(name = "idx_goods_receipt_received_at", columnList = "received_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsReceipt extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_no", nullable = false, length = 32)
    private String receiptNo;

    @Column(name = "order_uid",    nullable = false, length = 26) private String orderUid;
    @Column(name = "pharmacy_uid", nullable = false, length = 26) private String pharmacyUid;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Setter @Column(name = "received_by_username", length = 64) private String receivedByUsername;
    @Setter @Column(name = "delivery_note", length = 120)       private String deliveryNote;
    @Setter @Column(length = 500)                                private String notes;

    public GoodsReceipt(String receiptNo, String orderUid, String pharmacyUid,
                        String receivedByUsername, String deliveryNote, String notes) {
        this.receiptNo = receiptNo;
        this.orderUid = orderUid;
        this.pharmacyUid = pharmacyUid;
        this.receivedByUsername = receivedByUsername;
        this.deliveryNote = deliveryNote;
        this.notes = notes;
        this.receivedAt = Instant.now();
    }
}
