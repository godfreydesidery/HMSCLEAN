package com.otapp.hmis.engine.hr.payroll.domain;

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
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A payroll period — typically one calendar month, but the period dates
 * are explicit so off-cycle runs (mid-month adjustments, year-end
 * bonuses) are also supported. {@code label} is the user-facing name
 * (e.g. "May 2026", "Q2 2026 bonus"). Per-employee detail lives on
 * {@code PayrollItem} rows.
 */
@Entity
@Table(name = "hr_payroll_period",
       uniqueConstraints = @UniqueConstraint(name = "uk_hr_payroll_period_code", columnNames = "code"),
       indexes = @Index(name = "idx_hr_payroll_period_status", columnList = "status"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayrollPeriod extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String code;

    @Setter @Column(nullable = false, length = 80) private String label;

    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date",   nullable = false) private LocalDate endDate;

    @Setter @Column(nullable = false, length = 3) private String currency;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayrollPeriodStatus status = PayrollPeriodStatus.DRAFT;

    @Setter @Column(length = 500) private String note;

    @Setter @Column(name = "verified_at")   private Instant verifiedAt;
    @Setter @Column(name = "verified_by_username", length = 64) private String verifiedByUsername;
    @Setter @Column(name = "approved_at")   private Instant approvedAt;
    @Setter @Column(name = "approved_by_username", length = 64) private String approvedByUsername;
    @Setter @Column(name = "paid_at")       private Instant paidAt;
    @Setter @Column(name = "cancelled_at")  private Instant cancelledAt;
    @Setter @Column(name = "cancel_reason", length = 500) private String cancelReason;

    public PayrollPeriod(String code, String label, LocalDate startDate, LocalDate endDate, String currency) {
        if (code == null || code.isBlank())   throw new BusinessRuleException("code is required");
        if (label == null || label.isBlank()) throw new BusinessRuleException("label is required");
        if (startDate == null || endDate == null) {
            throw new BusinessRuleException("start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date cannot precede start date");
        }
        this.code = code.trim();
        this.label = label.trim();
        this.startDate = startDate;
        this.endDate = endDate;
        this.currency = (currency == null || currency.isBlank()) ? "TZS" : currency.trim();
    }

    public boolean isMutable() {
        return status == PayrollPeriodStatus.DRAFT;
    }

    /** Manager checkpoint: DRAFT → VERIFIED. Locks the items for director review. */
    public void verify(String username) {
        if (status != PayrollPeriodStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT periods can be verified (current: " + status + ")");
        }
        status = PayrollPeriodStatus.VERIFIED;
        verifiedAt = Instant.now();
        verifiedByUsername = username;
    }

    public void approve(String username) {
        if (status != PayrollPeriodStatus.VERIFIED) {
            throw new BusinessRuleException("Only VERIFIED periods can be approved (current: " + status + ")");
        }
        status = PayrollPeriodStatus.APPROVED;
        approvedAt = Instant.now();
        approvedByUsername = username;
    }

    public void markPaid() {
        if (status != PayrollPeriodStatus.APPROVED) {
            throw new BusinessRuleException("Only APPROVED periods can be paid (current: " + status + ")");
        }
        status = PayrollPeriodStatus.PAID;
        paidAt = Instant.now();
    }

    public void cancel(String reason) {
        if (status == PayrollPeriodStatus.PAID) {
            throw new BusinessRuleException("Paid periods cannot be cancelled");
        }
        if (status == PayrollPeriodStatus.CANCELLED) return;
        status = PayrollPeriodStatus.CANCELLED;
        cancelledAt = Instant.now();
        cancelReason = reason;
    }
}
