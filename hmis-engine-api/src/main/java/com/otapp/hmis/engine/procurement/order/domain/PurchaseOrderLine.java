package com.otapp.hmis.engine.procurement.order.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_order_line",
       indexes = {
               @Index(name = "idx_purchase_order_line_order",    columnList = "order_uid"),
               @Index(name = "idx_purchase_order_line_medicine", columnList = "medicine_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_uid",    nullable = false, length = 26) private String orderUid;
    @Column(name = "medicine_uid", nullable = false, length = 26) private String medicineUid;

    @Setter @Column(name = "ordered_quantity",  nullable = false) private int orderedQuantity;
    @Column(name = "received_quantity", nullable = false) private int receivedQuantity = 0;
    /** Cumulative units invoiced by supplier across all approved invoices on this line. */
    @Column(name = "invoiced_quantity", nullable = false) private int invoicedQuantity = 0;

    @Setter @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Setter @Column(length = 3, nullable = false) private String currency = "TZS";

    public PurchaseOrderLine(String orderUid, String medicineUid, int orderedQuantity,
                             BigDecimal unitCost, String currency) {
        this.orderUid = orderUid;
        this.medicineUid = medicineUid;
        this.orderedQuantity = orderedQuantity;
        this.unitCost = unitCost == null ? BigDecimal.ZERO : unitCost;
        this.currency = currency == null ? "TZS" : currency;
    }

    public int outstandingQuantity() {
        return Math.max(0, orderedQuantity - receivedQuantity);
    }

    public boolean isFullyReceived() {
        return receivedQuantity >= orderedQuantity;
    }

    public void recordReceipt(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Receipt quantity must be positive");
        }
        int outstanding = outstandingQuantity();
        if (quantity > outstanding) {
            throw new BusinessRuleException(
                    "Receipt of " + quantity + " exceeds outstanding " + outstanding);
        }
        this.receivedQuantity += quantity;
    }

    /** Outstanding receipt against invoiced quantity = received - invoiced. */
    public int uninvoicedReceivedQuantity() {
        return Math.max(0, receivedQuantity - invoicedQuantity);
    }

    /**
     * Records a supplier invoice line claim against this PO line.
     * Three-way match rule: can never invoice more than received (you
     * can't bill for goods we haven't acknowledged) and can never invoice
     * more than ordered (you can't bill for goods we didn't agree to buy).
     */
    public void recordInvoice(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Invoice quantity must be positive");
        }
        int outstandingReceipt = uninvoicedReceivedQuantity();
        if (quantity > outstandingReceipt) {
            throw new BusinessRuleException(
                    "Invoice of " + quantity + " exceeds uninvoiced-received " + outstandingReceipt
                            + " (received: " + receivedQuantity + ", already invoiced: " + invoicedQuantity + ")");
        }
        this.invoicedQuantity += quantity;
    }

    public BigDecimal lineAmount() {
        return unitCost.multiply(BigDecimal.valueOf(orderedQuantity));
    }
}
