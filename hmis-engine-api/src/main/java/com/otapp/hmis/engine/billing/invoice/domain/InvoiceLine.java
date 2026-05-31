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
        @Index(name = "idx_invoice_line_invoice", columnList = "invoice_uid"),
        @Index(name = "idx_invoice_line_principal", columnList = "principal_line_uid")
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

    /**
     * Per-line payer routing (legacy {@code PatientBill.status}). Defaults to
     * {@link LineCoverageStatus#UNPAID} — the cash path — and is promoted to
     * COVERED / VERIFIED by the charge-time coverage resolution.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_status", nullable = false, length = 16)
    private LineCoverageStatus coverageStatus = LineCoverageStatus.UNPAID;

    /** Stamped from the patient when COVERED (legacy {@code PatientBill.membershipNo}). */
    @Column(name = "membership_no", length = 64) private String membershipNo;

    /** The plan that covered this line; {@code null} for self-pay / cash lines. */
    @Column(name = "payer_plan_uid", length = 26) private String payerPlanUid;

    /**
     * Set on a supplementary ward top-up line, pointing at its COVERED principal
     * line (loose uid coupling — replaces the legacy principal/supplementary
     * self-FK). {@code null} on ordinary lines and on the principal itself.
     */
    @Column(name = "principal_line_uid", length = 26) private String principalLineUid;

    @SuppressWarnings("java:S107") // private — the public surface is the named factories below
    private InvoiceLine(String invoiceUid, InvoiceLineKind kind, String serviceUid, String referenceUid,
                        String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount,
                        LineCoverageStatus coverageStatus, String membershipNo, String payerPlanUid,
                        String principalLineUid) {
        this.invoiceUid = invoiceUid;
        this.kind = kind;
        this.serviceUid = serviceUid;
        this.referenceUid = referenceUid;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
        this.coverageStatus = coverageStatus;
        this.membershipNo = membershipNo;
        this.payerPlanUid = payerPlanUid;
        this.principalLineUid = principalLineUid;
    }

    /**
     * An ordinary (non-coverage-aware) line — cash UNPAID. Retained so existing
     * billing flows that do not resolve coverage stay unchanged.
     */
    @SuppressWarnings("java:S107")
    public InvoiceLine(String invoiceUid, InvoiceLineKind kind, String serviceUid, String referenceUid,
                       String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        this(invoiceUid, kind, serviceUid, referenceUid, description, quantity, unitPrice, amount,
                LineCoverageStatus.UNPAID, null, null, null);
    }

    /**
     * A coverage-routed charge line. {@code coverageStatus} is COVERED (insurer
     * pays, membership/plan stamped), VERIFIED (insured-but-uncovered inpatient
     * accrual, still owed), or UNPAID (cash). For COVERED lines pass the
     * {@code membershipNo} and {@code payerPlanUid}; leave them {@code null}
     * otherwise.
     */
    @SuppressWarnings("java:S107")
    public static InvoiceLine routed(String invoiceUid, InvoiceLineKind kind, String serviceUid, String referenceUid,
                                     String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount,
                                     LineCoverageStatus coverageStatus, String membershipNo, String payerPlanUid) {
        return new InvoiceLine(invoiceUid, kind, serviceUid, referenceUid, description, quantity, unitPrice, amount,
                coverageStatus, membershipNo, payerPlanUid, null);
    }

    /**
     * A supplementary ward top-up line (the cash remainder above the plan
     * ceiling). Always UNPAID and owed; linked to its COVERED principal via
     * {@code principalLineUid}.
     */
    @SuppressWarnings("java:S107")
    public static InvoiceLine supplementaryLine(String invoiceUid, InvoiceLineKind kind, String serviceUid,
                                                String referenceUid, String description, BigDecimal quantity,
                                                BigDecimal unitPrice, BigDecimal amount, String principalLineUid) {
        return new InvoiceLine(invoiceUid, kind, serviceUid, referenceUid, description, quantity, unitPrice, amount,
                LineCoverageStatus.UNPAID, null, null, principalLineUid);
    }
}
