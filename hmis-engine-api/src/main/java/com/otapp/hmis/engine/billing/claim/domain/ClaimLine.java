package com.otapp.hmis.engine.billing.claim.domain;

import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An immutable snapshot of one COVERED invoice line as it was claimed — a thin
 * pointer back to the source {@code invoice_line} plus the description / amount
 * captured at build time (mirrors legacy {@code PatientInvoiceDetail}). The
 * {@code UNIQUE(invoice_line_uid)} constraint is the DB-level guarantee a covered
 * line is claimed at most once.
 */
@Entity
@Table(name = "insurance_claim_lines",
       uniqueConstraints = {
               @UniqueConstraint(name = "uk_insurance_claim_lines_invoice_line", columnNames = "invoice_line_id")
       },
       indexes = {
               @Index(name = "idx_insurance_claim_lines_claim", columnList = "claim_id")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent claim (intra-billing FK -> insurance_claims.id). */
    @Column(name = "claim_id",        nullable = false) private Long claimId;
    /** The source COVERED line this snapshot was taken from (intra-billing FK -> invoice_line.id). */
    @Column(name = "invoice_line_id", nullable = false) private Long invoiceLineId;
    /** The claimed service (cross-module snapshot -> masterdata catalogue uid). */
    @Column(name = "service_uid",     length = 26) private String serviceUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceLineKind kind;

    @Column(nullable = false, length = 255) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;

    @SuppressWarnings("java:S107") // snapshot copy of one invoice line
    public ClaimLine(Long claimId, Long invoiceLineId, String serviceUid,
                     InvoiceLineKind kind, String description, BigDecimal quantity,
                     BigDecimal unitPrice, BigDecimal amount) {
        this.claimId = claimId;
        this.invoiceLineId = invoiceLineId;
        this.serviceUid = serviceUid;
        this.kind = kind;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }
}
