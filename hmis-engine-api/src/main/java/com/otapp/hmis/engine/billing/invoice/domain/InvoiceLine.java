package com.otapp.hmis.engine.billing.invoice.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoice_line", indexes = {
        @Index(name = "idx_invoice_line_invoice", columnList = "invoice_uid")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_uid", nullable = false, length = 26)
    private String invoiceUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceLineKind kind;

    @Column(name = "service_uid", length = 26) private String serviceUid;
    @Column(name = "reference_uid", length = 26) private String referenceUid;

    @Setter @Column(nullable = false, length = 255) private String description;
    @Setter @Column(nullable = false, precision = 12, scale = 2) private BigDecimal quantity;
    @Setter @Column(name = "unit_price", nullable = false, precision = 14, scale = 2) private BigDecimal unitPrice;
    @Setter @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;

    public InvoiceLine(String invoiceUid, InvoiceLineKind kind, String serviceUid, String referenceUid,
                       String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        this.invoiceUid = invoiceUid;
        this.kind = kind;
        this.serviceUid = serviceUid;
        this.referenceUid = referenceUid;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }
}
