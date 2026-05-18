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
               @UniqueConstraint(name = "uk_invoice_no", columnNames = "invoice_no")
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

    /** Set when the invoice is raised from an outpatient consultation. */
    @Column(name = "consultation_uid", length = 26) private String consultationUid;
    /** Set when the invoice is raised from an inpatient admission. */
    @Column(name = "admission_uid",    length = 26) private String admissionUid;
    @Column(name = "patient_uid",      nullable = false, length = 26) private String patientUid;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 16)
    private PaymentType paymentType;

    @Setter @Column(name = "insurance_plan_uid", length = 26) private String insurancePlanUid;

    @Setter @Column(nullable = false, length = 3) private String currency;
    @Setter @Column(nullable = false, precision = 14, scale = 2) private BigDecimal subtotal = BigDecimal.ZERO;
    @Setter @Column(name = "total_paid", nullable = false, precision = 14, scale = 2) private BigDecimal totalPaid = BigDecimal.ZERO;
    /** Sum of applied credit notes — write-downs that reduce the patient's outstanding balance. */
    @Setter @Column(name = "total_credited", nullable = false, precision = 14, scale = 2) private BigDecimal totalCredited = BigDecimal.ZERO;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Setter @Column(name = "issued_at")    private Instant issuedAt;
    @Setter @Column(name = "paid_at")      private Instant paidAt;
    @Setter @Column(name = "cancelled_at") private Instant cancelledAt;
    @Setter @Column(name = "cancel_reason", length = 255) private String cancelReason;

    private Invoice(String invoiceNo, String consultationUid, String admissionUid, String patientUid,
                    PaymentType paymentType, String insurancePlanUid, String currency) {
        if (consultationUid != null && admissionUid != null) {
            throw new BusinessRuleException(
                    "An invoice cannot reference both a consultation and an admission");
        }
        // both null is allowed — that's an OUTSIDER (walk-in) invoice keyed only by patient.
        this.invoiceNo = invoiceNo;
        this.consultationUid = consultationUid;
        this.admissionUid = admissionUid;
        this.patientUid = patientUid;
        this.paymentType = paymentType;
        this.insurancePlanUid = insurancePlanUid;
        this.currency = currency;
    }

    public static Invoice forConsultation(String invoiceNo, String consultationUid, String patientUid,
                                          PaymentType paymentType, String insurancePlanUid, String currency) {
        return new Invoice(invoiceNo, consultationUid, null, patientUid, paymentType, insurancePlanUid, currency);
    }

    public static Invoice forAdmission(String invoiceNo, String admissionUid, String patientUid,
                                       PaymentType paymentType, String insurancePlanUid, String currency) {
        return new Invoice(invoiceNo, null, admissionUid, patientUid, paymentType, insurancePlanUid, currency);
    }

    /** OUTSIDER walk-in invoice — keyed only by patient, no consultation or admission. */
    public static Invoice forOutsider(String invoiceNo, String patientUid,
                                      PaymentType paymentType, String insurancePlanUid, String currency) {
        return new Invoice(invoiceNo, null, null, patientUid, paymentType, insurancePlanUid, currency);
    }

    public boolean isOutsider() {
        return consultationUid == null && admissionUid == null;
    }

    /** What the patient still owes: billed amount minus cash received minus authorised write-downs. */
    public BigDecimal balance() {
        return subtotal.subtract(totalPaid).subtract(totalCredited);
    }

    /** Cash received + write-downs — used by status roll-ups so a fully-credited invoice settles. */
    public BigDecimal settledAmount() {
        return totalPaid.add(totalCredited);
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
        if (newTotal.add(totalCredited).compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Payment exceeds invoice balance");
        }
        totalPaid = newTotal;
        recomputeStatusAfterCredit();
    }

    /**
     * Applies an authorised write-down. Reduces balance without recording
     * cash. The caller (CreditNoteService) is responsible for the
     * {@link com.otapp.hmis.engine.billing.creditnote.domain.CreditNote}
     * audit record.
     */
    public void applyCreditNote(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Credit-note amount must be positive");
        }
        if (status == InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Cannot credit a DRAFT invoice (issue it first)");
        }
        if (status == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot credit a cancelled invoice");
        }
        BigDecimal newCredited = totalCredited.add(amount);
        if (newCredited.add(totalPaid).compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Credit-note exceeds invoice outstanding balance");
        }
        totalCredited = newCredited;
        recomputeStatusAfterCredit();
    }

    /**
     * Returns money against this invoice. Reduces {@link #totalPaid} and
     * may roll the status back from PAID to PARTIALLY_PAID / ISSUED.
     */
    public void applyRefund(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Refund amount must be positive");
        }
        if (status == InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Cannot refund a DRAFT invoice");
        }
        if (status == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot refund a cancelled invoice");
        }
        if (amount.compareTo(totalPaid) > 0) {
            throw new BusinessRuleException("Refund exceeds amount paid");
        }
        totalPaid = totalPaid.subtract(amount);
        // Status may need to roll back: settled-fully → partially → still issued.
        if (settledAmount().signum() == 0) {
            status = InvoiceStatus.ISSUED;
            paidAt = null;
        } else if (settledAmount().compareTo(subtotal) < 0) {
            status = InvoiceStatus.PARTIALLY_PAID;
            paidAt = null;
        }
    }

    private void recomputeStatusAfterCredit() {
        if (settledAmount().compareTo(subtotal) >= 0) {
            status = InvoiceStatus.PAID;
            paidAt = Instant.now();
        } else {
            status = InvoiceStatus.PARTIALLY_PAID;
        }
    }
}
