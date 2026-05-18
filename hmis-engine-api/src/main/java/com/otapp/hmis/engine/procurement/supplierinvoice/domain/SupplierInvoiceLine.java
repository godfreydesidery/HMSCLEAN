package com.otapp.hmis.engine.procurement.supplierinvoice.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line on a supplier invoice — references a specific PO line and
 * carries the invoiced quantity + amount. Three-way match validation
 * runs at approval against the PO line's ordered + received quantities.
 */
@Entity
@Table(name = "supplier_invoice_line",
       indexes = {
               @Index(name = "idx_supplier_invoice_line_invoice", columnList = "invoice_uid"),
               @Index(name = "idx_supplier_invoice_line_po_line", columnList = "po_line_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierInvoiceLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_uid", nullable = false, length = 26) private String invoiceUid;
    @Column(name = "po_line_uid", nullable = false, length = 26) private String poLineUid;

    @Setter @Column(name = "invoiced_quantity", nullable = false) private int invoicedQuantity;
    @Setter @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Setter @Column(name = "line_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineAmount = BigDecimal.ZERO;

    public SupplierInvoiceLine(String invoiceUid, String poLineUid,
                               int invoicedQuantity, BigDecimal unitCost) {
        if (invoicedQuantity <= 0) {
            throw new BusinessRuleException("Invoiced quantity must be positive");
        }
        if (unitCost == null || unitCost.signum() < 0) {
            throw new BusinessRuleException("Unit cost must be non-negative");
        }
        this.invoiceUid = invoiceUid;
        this.poLineUid = poLineUid;
        this.invoicedQuantity = invoicedQuantity;
        this.unitCost = unitCost;
        this.lineAmount = unitCost.multiply(BigDecimal.valueOf(invoicedQuantity));
    }
}
