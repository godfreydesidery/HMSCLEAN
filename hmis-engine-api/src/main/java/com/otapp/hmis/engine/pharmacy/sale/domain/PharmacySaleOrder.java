package com.otapp.hmis.engine.pharmacy.sale.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import com.otapp.hmis.engine.patient.domain.PaymentType;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A retail / OTC pharmacy sale. Multi-line; each line carries the proven
 * 8-state lifecycle (PROCESS.md §8.2). Customer can be a registered
 * OUTSIDER patient ({@link #patientUid}) or an anonymous walk-in
 * ({@link #customerName} only — required for audit).
 */
@Entity
@Table(name = "pharmacy_sale_order",
       uniqueConstraints = @UniqueConstraint(name = "uk_pharmacy_sale_order_no", columnNames = "sale_no"),
       indexes = {
               @Index(name = "idx_pharmacy_sale_order_pharmacy", columnList = "pharmacy_uid"),
               @Index(name = "idx_pharmacy_sale_order_patient",  columnList = "patient_uid"),
               @Index(name = "idx_pharmacy_sale_order_status",   columnList = "status"),
               @Index(name = "idx_pharmacy_sale_order_opened",   columnList = "opened_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacySaleOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable sale number, e.g. PSO-2026-000123. */
    @Column(name = "sale_no", nullable = false, length = 32)
    private String saleNo;

    /** Where the stock comes from. */
    @Column(name = "pharmacy_uid", nullable = false, length = 26)
    private String pharmacyUid;

    /** Optional: registered patient (OUTSIDER). */
    @Setter @Column(name = "patient_uid", length = 26) private String patientUid;

    /** Always required — audit trail even for walk-ins. */
    @Setter @Column(name = "customer_name", nullable = false, length = 160) private String customerName;
    @Setter @Column(name = "customer_phone", length = 40) private String customerPhone;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PharmacySaleOrderStatus status = PharmacySaleOrderStatus.ACTIVE;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 16)
    private PaymentType paymentType = PaymentType.CASH;

    @Setter @Column(name = "insurance_plan_uid", length = 26) private String insurancePlanUid;

    @Setter @Column(nullable = false, length = 3) private String currency = "TZS";
    @Setter @Column(nullable = false, precision = 14, scale = 2) private BigDecimal subtotal = BigDecimal.ZERO;
    @Setter @Column(name = "total_paid", nullable = false, precision = 14, scale = 2) private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "opened_at", nullable = false) private Instant openedAt;
    @Setter @Column(name = "completed_at") private Instant completedAt;
    @Setter @Column(name = "cancelled_at") private Instant cancelledAt;
    @Setter @Column(name = "cancel_reason", length = 255) private String cancelReason;

    public PharmacySaleOrder(String saleNo, String pharmacyUid, String customerName, String customerPhone,
                             String patientUid, PaymentType paymentType, String insurancePlanUid,
                             String currency) {
        this.saleNo = saleNo;
        this.pharmacyUid = pharmacyUid;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.patientUid = patientUid;
        this.paymentType = paymentType == null ? PaymentType.CASH : paymentType;
        this.insurancePlanUid = insurancePlanUid;
        this.currency = currency == null ? "TZS" : currency;
        this.openedAt = Instant.now();
    }

    public BigDecimal balance() {
        return subtotal.subtract(totalPaid);
    }

    /**
     * Roll-up — called after any line transitions so the header reflects
     * whether the sale is still in flight or done. Pass the count of lines
     * still in a non-terminal state.
     */
    public void onLineTransition(int openLines) {
        if (status == PharmacySaleOrderStatus.CANCELLED) {
            return;
        }
        if (openLines == 0) {
            status = PharmacySaleOrderStatus.COMPLETED;
            completedAt = Instant.now();
        } else if (status != PharmacySaleOrderStatus.ACTIVE) {
            status = PharmacySaleOrderStatus.ACTIVE;
            completedAt = null;
        }
    }

    public void cancel(String reason) {
        if (status == PharmacySaleOrderStatus.CANCELLED) return;
        if (status == PharmacySaleOrderStatus.COMPLETED) {
            throw new BusinessRuleException("Completed sales cannot be cancelled");
        }
        status = PharmacySaleOrderStatus.CANCELLED;
        cancelledAt = Instant.now();
        cancelReason = reason;
    }

    public void applyPayment(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new BusinessRuleException("Payment amount must be positive");
        }
        BigDecimal next = totalPaid.add(amount);
        if (next.compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Payment exceeds sale balance");
        }
        totalPaid = next;
    }
}
