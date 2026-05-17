package com.otapp.hmis.engine.procurement.receipt.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
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

    public GoodsReceiptLine(String receiptUid, String poLineUid, String medicineUid, int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Receipt line quantity must be positive");
        }
        this.receiptUid = receiptUid;
        this.poLineUid = poLineUid;
        this.medicineUid = medicineUid;
        this.quantity = quantity;
    }
}
