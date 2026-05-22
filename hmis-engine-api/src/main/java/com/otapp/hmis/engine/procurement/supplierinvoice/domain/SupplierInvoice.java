package com.otapp.hmis.engine.procurement.supplierinvoice.domain;

import com.otapp.hmis.engine.billing.payment.domain.PaymentMethod;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
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
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Supplier-issued invoice referenced against one of our purchase orders.
 * Holds the external invoice number, currency, amount and date; the
 * line-level three-way match validation lives on
 * {@link SupplierInvoiceLine}.
 *
 * <p>One PO may have many supplier invoices (partial deliveries → split
 * billing). {@code (supplier_uid, supplier_invoice_no)} is unique to
 * prevent the same external invoice being recorded twice.
 */
@Entity
@Table(name = "supplier_invoice",
       uniqueConstraints = {
               @UniqueConstraint(name = "uk_supplier_invoice_uid", columnNames = "uid"),
               @UniqueConstraint(name = "uk_supplier_invoice_supplier_no",
                                 columnNames = {"supplier_uid", "supplier_invoice_no"})
       },
       indexes = {
               @Index(name = "idx_supplier_invoice_supplier", columnList = "supplier_uid"),
               @Index(name = "idx_supplier_invoice_order",    columnList = "order_uid"),
               @Index(name = "idx_supplier_invoice_status",   columnList = "status"),
               @Index(name = "idx_supplier_invoice_date",     columnList = "invoice_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierInvoice extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_uid", nullable = false, length = 26) private String supplierUid;
    @Column(name = "order_uid",    nullable = false, length = 26) private String orderUid;

    /** External invoice number as printed by the supplier. */
    @Column(name = "supplier_invoice_no", nullable = false, length = 64)
    private String supplierInvoiceNo;

    @Column(name = "invoice_date", nullable = false) private LocalDate invoiceDate;
    @Setter @Column(name = "due_date")               private LocalDate dueDate;

    @Setter @Column(nullable = false, length = 3) private String currency = "TZS";
    @Setter @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SupplierInvoiceStatus status = SupplierInvoiceStatus.DRAFT;

    @Setter @Column(name = "submitted_at")              private Instant submittedAt;
    @Setter @Column(name = "submitted_by_username", length = 64) private String submittedByUsername;
    @Setter @Column(name = "approved_at")               private Instant approvedAt;
    @Setter @Column(name = "approved_by_username",  length = 64) private String approvedByUsername;
    @Setter @Column(name = "paid_at")                   private Instant paidAt;
    @Setter @Column(name = "paid_by_username",      length = 64) private String paidByUsername;
    @Setter @Column(name = "rejected_at")               private Instant rejectedAt;
    @Setter @Column(name = "rejected_by_username",  length = 64) private String rejectedByUsername;
    @Setter @Column(name = "reject_reason", length = 255)        private String rejectReason;
    @Setter @Column(name = "cancelled_at")              private Instant cancelledAt;

    /** Payment details, captured on markPaid. */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 24)
    private PaymentMethod paymentMethod;

    @Setter @Column(name = "payment_reference", length = 120) private String paymentReference;

    @Setter @Column(length = 500) private String notes;

    public SupplierInvoice(String supplierUid, String orderUid, String supplierInvoiceNo,
                           LocalDate invoiceDate, LocalDate dueDate,
                           String currency, String notes) {
        if (supplierInvoiceNo == null || supplierInvoiceNo.isBlank()) {
            throw new BusinessRuleException("Supplier invoice number is required");
        }
        if (invoiceDate == null) {
            throw new BusinessRuleException("Invoice date is required");
        }
        this.supplierUid = supplierUid;
        this.orderUid = orderUid;
        this.supplierInvoiceNo = supplierInvoiceNo;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.currency = (currency == null || currency.isBlank()) ? "TZS" : currency;
        this.notes = notes;
    }

    public boolean isEditable() {
        return status == SupplierInvoiceStatus.DRAFT;
    }

    public void submit(String username) {
        if (status != SupplierInvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT invoices can be submitted (current: " + status + ")");
        }
        status = SupplierInvoiceStatus.SUBMITTED;
        submittedAt = Instant.now();
        submittedByUsername = username;
    }

    public void approve(String username) {
        if (status != SupplierInvoiceStatus.SUBMITTED) {
            throw new BusinessRuleException("Only SUBMITTED invoices can be approved (current: " + status + ")");
        }
        status = SupplierInvoiceStatus.APPROVED;
        approvedAt = Instant.now();
        approvedByUsername = username;
    }

    public void markPaid(String username, PaymentMethod method, String reference) {
        if (status != SupplierInvoiceStatus.APPROVED) {
            throw new BusinessRuleException("Only APPROVED invoices can be paid (current: " + status + ")");
        }
        if (method == null) {
            throw new BusinessRuleException("Payment method is required");
        }
        status = SupplierInvoiceStatus.PAID;
        paidAt = Instant.now();
        paidByUsername = username;
        paymentMethod = method;
        paymentReference = reference;
    }

    public void reject(String username, String reason) {
        if (status != SupplierInvoiceStatus.SUBMITTED) {
            throw new BusinessRuleException("Only SUBMITTED invoices can be rejected (current: " + status + ")");
        }
        status = SupplierInvoiceStatus.REJECTED;
        rejectedAt = Instant.now();
        rejectedByUsername = username;
        rejectReason = reason;
    }

    public void cancel() {
        if (status == SupplierInvoiceStatus.CANCELLED) return;
        if (status != SupplierInvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT invoices can be cancelled (current: " + status + ")");
        }
        status = SupplierInvoiceStatus.CANCELLED;
        cancelledAt = Instant.now();
    }
}
