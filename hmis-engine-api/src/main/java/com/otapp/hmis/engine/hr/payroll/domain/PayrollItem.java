package com.otapp.hmis.engine.hr.payroll.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.Setter;

/**
 * One employee's pay for a {@link PayrollPeriod}. Gross + total
 * deductions are captured as snapshot figures (no statutory tax tables
 * or auto-prefill in this V1 — those live in a real HR product). Net is
 * computed at write-time so any subsequent edit re-derives it.
 */
@Entity
@Table(name = "hr_payroll_item",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_hr_payroll_item_period_employee",
               columnNames = {"period_uid", "employee_uid"}),
       indexes = {
               @Index(name = "idx_hr_payroll_item_period",   columnList = "period_uid"),
               @Index(name = "idx_hr_payroll_item_employee", columnList = "employee_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayrollItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_uid",   nullable = false, length = 26) private String periodUid;
    @Column(name = "employee_uid", nullable = false, length = 26) private String employeeUid;

    @Setter @Column(name = "gross_pay",         nullable = false, precision = 14, scale = 2) private BigDecimal grossPay;
    @Setter @Column(name = "total_deductions",  nullable = false, precision = 14, scale = 2) private BigDecimal totalDeductions;
    @Setter @Column(name = "net_pay",           nullable = false, precision = 14, scale = 2) private BigDecimal netPay;

    /**
     * Employer-side cost of employment (legacy {@code PayrollDetail.employer-
     * Contributions}). Tracked verbatim for the cost report; NOT in the net
     * formula and NOT part of gross — {@link #recomputeNet()} never touches it.
     */
    @Setter @Column(name = "employer_contributions", nullable = false, precision = 14, scale = 2)
    private BigDecimal employerContributions = BigDecimal.ZERO;

    @Setter @Column(name = "payment_method",    length = 32)  private String paymentMethod;
    @Setter @Column(name = "payment_reference", length = 80)  private String paymentReference;
    @Setter @Column(name = "note",              length = 500) private String note;

    public PayrollItem(String periodUid, String employeeUid, BigDecimal grossPay, BigDecimal totalDeductions) {
        if (grossPay == null || grossPay.signum() < 0) {
            throw new BusinessRuleException("Gross pay must be non-negative");
        }
        if (totalDeductions == null || totalDeductions.signum() < 0) {
            throw new BusinessRuleException("Total deductions must be non-negative");
        }
        if (totalDeductions.compareTo(grossPay) > 0) {
            throw new BusinessRuleException("Deductions cannot exceed gross pay");
        }
        this.periodUid = periodUid;
        this.employeeUid = employeeUid;
        this.grossPay = grossPay;
        this.totalDeductions = totalDeductions;
        this.netPay = grossPay.subtract(totalDeductions);
    }

    /** Re-derive {@link #netPay} after a gross / deductions edit. */
    public void recomputeNet() {
        if (totalDeductions.compareTo(grossPay) > 0) {
            throw new BusinessRuleException("Deductions cannot exceed gross pay");
        }
        this.netPay = grossPay.subtract(totalDeductions);
    }
}
