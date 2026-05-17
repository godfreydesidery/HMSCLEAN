package com.otapp.hmis.engine.billing.invoice.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoice",
       uniqueConstraints = {
               @UniqueConstraint(name = "uk_invoice_no",            columnNames = "invoice_no"),
               @UniqueConstraint(name = "uk_invoice_consultation",  columnNames = "consultation_uid")
       },
       indexes = {
               @Index(name = "idx_invoice_patient",   columnList = "patient_uid"),
               @Index(name = "idx_invoice_status",    columnList = "status"),
               @Index(name = "idx_invoice_issued_at", columnList = "issued_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_no", nullable = false, length = 32)
    private String invoiceNo;

    @Column(name = "consultation_uid", nullable = false, length = 26) private String consultationUid;
    @Column(name = "patient_uid",      nullable = false, length = 26) private String patientUid;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 16)
    private PaymentType paymentType;

    @Setter @Column(name = "insurance_plan_uid", length = 26) private String insurancePlanUid;

    @Setter @Column(nullable = false, length = 3) private String currency;
    @Setter @Column(nullable = false, precision = 14, scale = 2) private BigDecimal subtotal = BigDecimal.ZERO;
    @Setter @Column(name = "total_paid", nullable = false, precision = 14, scale = 2) private BigDecimal totalPaid = BigDecimal.ZERO;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Setter @Column(name = "issued_at")    private Instant issuedAt;
    @Setter @Column(name = "paid_at")      private Instant paidAt;
    @Setter @Column(name = "cancelled_at") private Instant cancelledAt;
    @Setter @Column(name = "cancel_reason", length = 255) private String cancelReason;

    public Invoice(String invoiceNo, String consultationUid, String patientUid,
                   PaymentType paymentType, String insurancePlanUid, String currency) {
        this.invoiceNo = invoiceNo;
        this.consultationUid = consultationUid;
        this.patientUid = patientUid;
        this.paymentType = paymentType;
        this.insurancePlanUid = insurancePlanUid;
        this.currency = currency;
    }

    public BigDecimal balance() {
        return subtotal.subtract(totalPaid);
    }

    public void issue() {
        if (status != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT invoices can be issued (current: " + status + ")");
        }
        status = InvoiceStatus.ISSUED;
        issuedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (status == InvoiceStatus.PAID) {
            throw new BusinessRuleException("Paid invoices cannot be cancelled");
        }
        status = InvoiceStatus.CANCELLED;
        cancelledAt = Instant.now();
        cancelReason = reason;
    }

    public void applyPayment(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new BusinessRuleException("Payment amount must be positive");
        }
        if (status == InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Cannot apply payment to a DRAFT invoice (issue it first)");
        }
        if (status == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot apply payment to a cancelled invoice");
        }
        BigDecimal newTotal = totalPaid.add(amount);
        if (newTotal.compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Payment exceeds invoice balance");
        }
        totalPaid = newTotal;
        if (totalPaid.compareTo(subtotal) >= 0) {
            status = InvoiceStatus.PAID;
            paidAt = Instant.now();
        } else {
            status = InvoiceStatus.PARTIALLY_PAID;
        }
    }
}
