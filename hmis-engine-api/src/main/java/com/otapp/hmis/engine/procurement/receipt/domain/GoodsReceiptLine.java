package com.otapp.hmis.engine.procurement.receipt.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "goods_receipt_line",
       indexes = {
               @Index(name = "idx_goods_receipt_line_receipt", columnList = "receipt_uid"),
               @Index(name = "idx_goods_receipt_line_po_line", columnList = "po_line_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsReceiptLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_uid",  nullable = false, length = 26) private String receiptUid;
    @Column(name = "po_line_uid",  nullable = false, length = 26) private String poLineUid;
    @Column(name = "medicine_uid", nullable = false, length = 26) private String medicineUid;

    @Column(nullable = false)
    private int quantity;

    /** Supplier-issued batch / lot number captured at goods-receipt time. */
    @Column(name = "batch_no", nullable = false, length = 64)
    private String batchNo;

    /** Supplier-stated manufactured date for the batch (legacy GRN parity). */
    @Column(name = "manufactured_date")
    private LocalDate manufacturedDate;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    public GoodsReceiptLine(String receiptUid, String poLineUid, String medicineUid,
                            int quantity, String batchNo, LocalDate manufacturedDate, LocalDate expiresAt) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Receipt line quantity must be positive");
        }
        if (batchNo == null || batchNo.isBlank()) {
            throw new BusinessRuleException("Receipt line must specify a batch number");
        }
        if (manufacturedDate != null && expiresAt != null && manufacturedDate.isAfter(expiresAt)) {
            throw new BusinessRuleException("Manufactured date cannot be after expiry date");
        }
        this.receiptUid = receiptUid;
        this.poLineUid = poLineUid;
        this.medicineUid = medicineUid;
        this.quantity = quantity;
        this.batchNo = batchNo.trim();
        this.manufacturedDate = manufacturedDate;
        this.expiresAt = expiresAt;
    }

    /** Back-compat overload without a manufactured date. */
    public GoodsReceiptLine(String receiptUid, String poLineUid, String medicineUid,
                            int quantity, String batchNo, LocalDate expiresAt) {
        this(receiptUid, poLineUid, medicineUid, quantity, batchNo, null, expiresAt);
    }
}
