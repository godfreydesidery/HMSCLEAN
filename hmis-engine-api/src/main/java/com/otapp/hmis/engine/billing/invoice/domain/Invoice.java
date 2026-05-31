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
    /**
     * Sum of insurer-covered line amounts — billed on this invoice but paid by the
     * patient's plan up front, so they do NOT count toward what the patient owes.
     * Kept separate from {@link #totalPaid} (cash) and {@link #totalCredited}
     * (write-downs) so each settlement channel is auditable on its own.
     */
    @Setter @Column(name = "total_covered", nullable = false, precision = 14, scale = 2) private BigDecimal totalCovered = BigDecimal.ZERO;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceScope scope;

    @Setter @Column(name = "issued_at")    private Instant issuedAt;
    @Setter @Column(name = "paid_at")      private Instant paidAt;
    @Setter @Column(name = "cancelled_at") private Instant cancelledAt;
    @Setter @Column(name = "cancel_reason", length = 255) private String cancelReason;

    @SuppressWarnings("java:S107") // private constructor; the 4 named factories keep the public surface narrow
    private Invoice(String invoiceNo, InvoiceScope scope, String consultationUid, String admissionUid,
                    String patientUid, PaymentType paymentType, String insurancePlanUid, String currency) {
        if (consultationUid != null && admissionUid != null) {
            throw new BusinessRuleException(
                    "An invoice cannot reference both a consultation and an admission");
        }
        // both null is allowed — that's an OUTSIDER or REGISTRATION invoice keyed only by patient.
        this.invoiceNo = invoiceNo;
        this.scope = scope;
        this.consultationUid = consultationUid;
        this.admissionUid = admissionUid;
        this.patientUid = patientUid;
        this.paymentType = paymentType;
        this.insurancePlanUid = insurancePlanUid;
        this.currency = currency;
    }

    public static Invoice forConsultation(String invoiceNo, String consultationUid, String patientUid,
                                          PaymentType paymentType, String insurancePlanUid, String currency) {
        return new Invoice(invoiceNo, InvoiceScope.CONSULTATION,
                consultationUid, null, patientUid, paymentType, insurancePlanUid, currency);
    }

    public static Invoice forAdmission(String invoiceNo, String admissionUid, String patientUid,
                                       PaymentType paymentType, String insurancePlanUid, String currency) {
        return new Invoice(invoiceNo, InvoiceScope.ADMISSION,
                null, admissionUid, patientUid, paymentType, insurancePlanUid, currency);
    }

    /** OUTSIDER walk-in invoice — keyed only by patient, no consultation or admission. */
    public static Invoice forOutsider(String invoiceNo, String patientUid,
                                      PaymentType paymentType, String insurancePlanUid, String currency) {
        return new Invoice(invoiceNo, InvoiceScope.OUTSIDER,
                null, null, patientUid, paymentType, insurancePlanUid, currency);
    }

    /** Registration fee invoice — one per patient, generated at registration time. */
    public static Invoice forRegistration(String invoiceNo, String patientUid,
                                          PaymentType paymentType, String insurancePlanUid, String currency) {
        return new Invoice(invoiceNo, InvoiceScope.REGISTRATION,
                null, null, patientUid, paymentType, insurancePlanUid, currency);
    }

    public boolean isOutsider() {
        return scope == InvoiceScope.OUTSIDER;
    }

    public boolean isRegistration() {
        return scope == InvoiceScope.REGISTRATION;
    }

    /**
     * What the patient still owes: billed amount minus cash received, minus
     * authorised write-downs, minus what the insurer covers. The covered portion
     * is netted out so an insured patient is never billed for a covered service.
     */
    public BigDecimal balance() {
        return subtotal.subtract(totalPaid).subtract(totalCredited).subtract(totalCovered);
    }

    /**
     * Everything that settles the invoice — cash received + write-downs +
     * insurer-covered — used by status roll-ups so a fully-covered (or
     * fully-credited) invoice reaches PAID.
     */
    public BigDecimal settledAmount() {
        return totalPaid.add(totalCredited).add(totalCovered);
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
        if (newTotal.add(totalCredited).add(totalCovered).compareTo(subtotal) > 0) {
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
        if (newCredited.add(totalPaid).add(totalCovered).compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Credit-note exceeds invoice outstanding balance");
        }
        totalCredited = newCredited;
        recomputeStatusAfterCredit();
    }

    /**
     * Records that the patient's insurer covers (pays) {@code amount} of this
     * invoice — billed on a COVERED line but settled by the plan up front. Adds
     * to {@link #totalCovered} so the covered portion nets out of the patient
     * balance, and rolls the status forward like a payment / credit. Mirrors the
     * legacy covered bill landing at balance 0, settled by the scheme. Called as
     * the COVERED line is added, after its amount is already in the subtotal.
     */
    public void recordInsurerCovered(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new BusinessRuleException("Insurer-covered amount must be non-negative");
        }
        if (status == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot record insurer coverage on a cancelled invoice");
        }
        totalCovered = totalCovered.add(amount);
        // A DRAFT invoice accrues coverage silently; its status moves on issue /
        // settlement. An issued invoice rolls forward (PARTIALLY_PAID / PAID).
        if (status != InvoiceStatus.DRAFT) {
            recomputeStatusAfterCredit();
        }
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

    /**
     * Reduce the subtotal (e.g. sign-out voids unpaid downstream lines) and
     * roll the status to match: a non-terminal invoice whose remaining balance
     * is now covered becomes PAID; if something is settled but a balance
     * remains it is PARTIALLY_PAID; otherwise it stays ISSUED. DRAFT/CANCELLED
     * are left untouched. Only reduces — never raises — the subtotal.
     */
    public void reduceSubtotalTo(BigDecimal newSubtotal) {
        if (newSubtotal == null || newSubtotal.signum() < 0) {
            throw new BusinessRuleException("Subtotal must be non-negative");
        }
        if (newSubtotal.compareTo(subtotal) > 0) {
            throw new BusinessRuleException("reduceSubtotalTo can only reduce the subtotal");
        }
        subtotal = newSubtotal;
        if (status == InvoiceStatus.DRAFT || status == InvoiceStatus.CANCELLED) {
            return;
        }
        if (settledAmount().compareTo(subtotal) >= 0) {
            status = InvoiceStatus.PAID;
            if (paidAt == null) {
                paidAt = Instant.now();
            }
        } else if (settledAmount().signum() > 0) {
            status = InvoiceStatus.PARTIALLY_PAID;
        } else {
            status = InvoiceStatus.ISSUED;
        }
    }
}
