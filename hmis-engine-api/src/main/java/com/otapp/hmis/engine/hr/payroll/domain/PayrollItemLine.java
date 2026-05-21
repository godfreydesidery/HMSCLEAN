package com.otapp.hmis.engine.hr.payroll.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One itemised earning / deduction line on a {@link PayrollItem} — the legacy
 * {@code PayrollDetail} breakdown (PROCESS_MISMATCHES.md M24). Captures what
 * makes up an employee's gross + deductions (basic, allowances, PAYE, NHIF,
 * loan, …) for transparency and audit. Typically populated from the
 * {@code PayrollComponent} compute, but stored as a snapshot so later rate
 * changes don't rewrite history.
 */
@Entity
@Table(name = "hr_payroll_item_line",
       indexes = @Index(name = "idx_hr_payroll_item_line_item", columnList = "item_uid"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayrollItemLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_uid", nullable = false, length = 26) private String itemUid;

    @Column(name = "code", length = 32)  private String code;
    @Column(name = "name", nullable = false, length = 120) private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private PayrollComponentType type;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    public PayrollItemLine(String itemUid, String code, String name,
                           PayrollComponentType type, BigDecimal amount, int sortOrder) {
        this.itemUid = itemUid;
        this.code = code;
        this.name = name;
        this.type = type;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.sortOrder = sortOrder;
    }
}
