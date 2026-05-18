package com.otapp.hmis.engine.pharmacy.sale.domain;

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
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line on a pharmacy sale order: a medicine + quantity + price, with
 * the proven 8-state pharmacy lifecycle. Stock decrement happens when the
 * line moves to SOLD (driven by {@link com.otapp.hmis.engine.pharmacy.stock.application.StockService}).
 */
@Entity
@Table(name = "pharmacy_sale_order_line",
       indexes = {
               @Index(name = "idx_pharmacy_sale_line_sale",     columnList = "sale_uid"),
               @Index(name = "idx_pharmacy_sale_line_medicine", columnList = "medicine_uid"),
               @Index(name = "idx_pharmacy_sale_line_status",   columnList = "status")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacySaleOrderLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_uid",     nullable = false, length = 26) private String saleUid;
    @Column(name = "medicine_uid", nullable = false, length = 26) private String medicineUid;

    @Setter @Column(nullable = false) private int quantity;
    @Setter @Column(name = "dose",         length = 80) private String dose;
    @Setter @Column(name = "frequency",    length = 80) private String frequency;
    @Setter @Column(name = "duration_days") private Integer durationDays;
    @Setter @Column(length = 500) private String instructions;

    @Setter @Column(name = "unit_price",  nullable = false, precision = 14, scale = 2) private BigDecimal unitPrice = BigDecimal.ZERO;
    @Setter @Column(name = "line_amount", nullable = false, precision = 14, scale = 2) private BigDecimal lineAmount = BigDecimal.ZERO;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PharmacySaleLineStatus status = PharmacySaleLineStatus.PENDING;

    @Column(name = "created_at_line", nullable = false) private Instant createdAtLine;
    @Setter @Column(name = "accepted_at")  private Instant acceptedAt;
    @Setter @Column(name = "held_at")      private Instant heldAt;
    @Setter @Column(name = "verified_at")  private Instant verifiedAt;
    @Setter @Column(name = "approved_at")  private Instant approvedAt;
    @Setter @Column(name = "sold_at")      private Instant soldAt;
    @Setter @Column(name = "rejected_at")  private Instant rejectedAt;
    @Setter @Column(name = "reject_reason",  length = 255) private String rejectReason;
    @Setter @Column(name = "cancel_reason",  length = 255) private String cancelReason;

    public PharmacySaleOrderLine(String saleUid, String medicineUid, int quantity, String dose,
                                 String frequency, Integer durationDays, String instructions,
                                 BigDecimal unitPrice) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Sale line quantity must be positive");
        }
        this.saleUid = saleUid;
        this.medicineUid = medicineUid;
        this.quantity = quantity;
        this.dose = dose;
        this.frequency = frequency;
        this.durationDays = durationDays;
        this.instructions = instructions;
        this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        this.lineAmount = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.createdAtLine = Instant.now();
    }

    public void accept() {
        if (status != PharmacySaleLineStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING lines can be accepted (current: " + status + ")");
        }
        status = PharmacySaleLineStatus.ACCEPTED;
        acceptedAt = Instant.now();
    }

    public void hold() {
        if (status != PharmacySaleLineStatus.ACCEPTED && status != PharmacySaleLineStatus.VERIFIED) {
            throw new BusinessRuleException("Cannot hold from " + status);
        }
        status = PharmacySaleLineStatus.HELD;
        heldAt = Instant.now();
    }

    public void verify() {
        if (status != PharmacySaleLineStatus.ACCEPTED && status != PharmacySaleLineStatus.HELD) {
            throw new BusinessRuleException("Cannot verify from " + status);
        }
        status = PharmacySaleLineStatus.VERIFIED;
        verifiedAt = Instant.now();
    }

    public void approve() {
        if (status != PharmacySaleLineStatus.VERIFIED) {
            throw new BusinessRuleException("Only VERIFIED lines can be approved (current: " + status + ")");
        }
        status = PharmacySaleLineStatus.APPROVED;
        approvedAt = Instant.now();
    }

    public void markSold() {
        if (status != PharmacySaleLineStatus.APPROVED) {
            throw new BusinessRuleException("Only APPROVED lines can be sold (current: " + status + ")");
        }
        status = PharmacySaleLineStatus.SOLD;
        soldAt = Instant.now();
    }

    public void reject(String reason) {
        if (isTerminal()) {
            throw new BusinessRuleException("Cannot reject from " + status);
        }
        status = PharmacySaleLineStatus.REJECTED;
        rejectedAt = Instant.now();
        rejectReason = reason;
    }

    public void cancel(String reason) {
        if (status == PharmacySaleLineStatus.SOLD) {
            throw new BusinessRuleException("Sold lines cannot be cancelled");
        }
        if (status == PharmacySaleLineStatus.CANCELLED) return;
        status = PharmacySaleLineStatus.CANCELLED;
        cancelReason = reason;
    }

    public boolean isTerminal() {
        return status == PharmacySaleLineStatus.SOLD
                || status == PharmacySaleLineStatus.REJECTED
                || status == PharmacySaleLineStatus.CANCELLED;
    }
}
